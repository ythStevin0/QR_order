package com.qrorder.ordering.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrorder.catalog.domain.DiningTable;
import com.qrorder.catalog.domain.MenuItem;
import com.qrorder.catalog.infrastructure.DiningTableRepository;
import com.qrorder.catalog.infrastructure.MenuItemRepository;
import com.qrorder.ordering.api.dto.CreateOrderRequest;
import com.qrorder.ordering.api.dto.OrderItemRequest;
import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.ordering.domain.*;
import com.qrorder.ordering.infrastructure.DailyCounterRepository;
import com.qrorder.ordering.infrastructure.OrderRepository;
import com.qrorder.outlet.domain.Outlet;
import com.qrorder.outlet.infrastructure.OutletRepository;
import com.qrorder.shared.error.BadRequestException;
import com.qrorder.shared.error.IdempotencyConflictException;
import com.qrorder.shared.error.ResourceNotFoundException;
import com.qrorder.shared.idempotency.IdempotencyKey;
import com.qrorder.shared.idempotency.IdempotencyKeyRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

@Service
public class OrderServiceImpl implements OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderServiceImpl.class);

    private final OrderRepository orderRepository;
    private final MenuItemRepository menuItemRepository;
    private final DiningTableRepository diningTableRepository;
    private final OutletRepository outletRepository;
    private final DailyCounterRepository dailyCounterRepository;
    private final IdempotencyKeyRepository idempotencyKeyRepository;
    private final ObjectMapper objectMapper;

    public OrderServiceImpl(OrderRepository orderRepository,
                            MenuItemRepository menuItemRepository,
                            DiningTableRepository diningTableRepository,
                            OutletRepository outletRepository,
                            DailyCounterRepository dailyCounterRepository,
                            IdempotencyKeyRepository idempotencyKeyRepository,
                            ObjectMapper objectMapper) {
        this.orderRepository = orderRepository;
        this.menuItemRepository = menuItemRepository;
        this.diningTableRepository = diningTableRepository;
        this.outletRepository = outletRepository;
        this.dailyCounterRepository = dailyCounterRepository;
        this.idempotencyKeyRepository = idempotencyKeyRepository;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public OrderResponse createOrder(CreateOrderRequest request, String idempotencyKey) {
        log.info("Processing order creation [outletId={}, type={}]", request.outletId(), request.orderType());

        // 1. Idempotency Check (FR-C9)
        String requestHash = null;
        if (idempotencyKey != null && !idempotencyKey.isBlank()) {
            requestHash = computeHash(request);
            Optional<IdempotencyKey> existingKey = idempotencyKeyRepository.findById(idempotencyKey);
            if (existingKey.isPresent()) {
                IdempotencyKey key = existingKey.get();
                if (!key.getRequestHash().equals(requestHash)) {
                    log.warn("Idempotency conflict for key={}", idempotencyKey);
                    throw new IdempotencyConflictException("Payload mismatch for Idempotency-Key: " + idempotencyKey);
                }
                log.info("Returning cached order response for Idempotency-Key={}", idempotencyKey);
                try {
                    return objectMapper.readValue(key.getResponseBody(), OrderResponse.class);
                } catch (JsonProcessingException e) {
                    log.error("Failed to deserialize cached idempotency response", e);
                }
            }
        }

        // 2. Validate Outlet
        Outlet outlet = outletRepository.findById(request.outletId())
                .orElseThrow(() -> new ResourceNotFoundException("Outlet not found with id: " + request.outletId()));

        if (!outlet.isOpen()) {
            throw new BadRequestException("Outlet '%s' is currently closed for orders".formatted(outlet.getName()));
        }

        // 3. Validate Context (DINE_IN vs TAKE_AWAY)
        String tableNumber = null;
        String customerName = null;
        Integer queueNumber = null;

        if (request.orderType() == OrderType.DINE_IN) {
            if (request.tableNumber() == null || request.tableNumber().isBlank()) {
                throw new BadRequestException("tableNumber is required for DINE_IN orders");
            }
            DiningTable table = diningTableRepository.findByOutletIdAndTableNumber(outlet.getId(), request.tableNumber().trim())
                    .orElseThrow(() -> new ResourceNotFoundException("Table '%s' not found in outlet %d".formatted(request.tableNumber(), outlet.getId())));
            if (!table.isActive()) {
                throw new BadRequestException("Table '%s' is currently inactive".formatted(table.getTableNumber()));
            }
            tableNumber = table.getTableNumber();
        } else {
            if (request.customerName() == null || request.customerName().isBlank()) {
                throw new BadRequestException("customerName is required for TAKE_AWAY orders");
            }
            customerName = request.customerName().trim();

            // Atomically acquire daily queue number with pessimistic lock
            LocalDate today = LocalDate.now();
            DailyCounter counter = dailyCounterRepository.findByOutletIdAndCounterDateWithLock(outlet.getId(), today)
                    .orElseGet(() -> dailyCounterRepository.save(new DailyCounter(outlet.getId(), today)));
            queueNumber = counter.incrementAndGet();
            dailyCounterRepository.save(counter);
        }

        // 4. Create Order Aggregate
        Order order = new Order(outlet, request.orderType(), tableNumber, customerName);
        order.setNotes(request.notes());
        order.setQueueNumber(queueNumber);

        // Record initial status history
        order.getStatusHistory().add(new OrderStatusHistory(order, null, OrderStatus.PENDING_PAYMENT, "SYSTEM"));

        // 5. Add lines with server-side price snapshots
        for (OrderItemRequest itemReq : request.items()) {
            MenuItem menuItem = menuItemRepository.findById(itemReq.menuItemId())
                    .orElseThrow(() -> new ResourceNotFoundException("Menu item not found with id: " + itemReq.menuItemId()));

            if (!menuItem.getOutlet().getId().equals(outlet.getId())) {
                throw new BadRequestException("Menu item '%s' does not belong to outlet %d".formatted(menuItem.getName(), outlet.getId()));
            }
            if (!menuItem.isAvailable()) {
                throw new BadRequestException("Menu item '%s' is currently sold out".formatted(menuItem.getName()));
            }

            order.addLine(menuItem.getId(), menuItem.getName(), menuItem.getPrice(), itemReq.quantity(), itemReq.notes());
        }

        // 6. Calculate total 100% on server
        order.calculateTotals(outlet.getTaxRate(), outlet.getServiceChargeRate());

        Order savedOrder = orderRepository.save(order);
        log.info("Order created successfully [id={}, outletId={}, type={}, total={}]",
                savedOrder.getId(), outlet.getId(), savedOrder.getOrderType(), savedOrder.getTotal());

        OrderResponse response = OrderResponse.from(savedOrder);

        // 7. Save Idempotency response if key was provided
        if (idempotencyKey != null && !idempotencyKey.isBlank() && requestHash != null) {
            try {
                String responseBody = objectMapper.writeValueAsString(response);
                Instant expiresAt = Instant.now().plus(24, ChronoUnit.HOURS);
                IdempotencyKey keyRecord = new IdempotencyKey(idempotencyKey, requestHash, responseBody, 201, expiresAt);
                idempotencyKeyRepository.save(keyRecord);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize response for idempotency storage", e);
            }
        }

        return response;
    }

    @Override
    @Transactional(readOnly = true)
    public OrderResponse getOrderById(UUID orderId) {
        log.info("Fetching order details [id={}]", orderId);
        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));
        return OrderResponse.from(order);
    }

    private String computeHash(CreateOrderRequest request) {
        try {
            String json = objectMapper.writeValueAsString(request);
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (JsonProcessingException | NoSuchAlgorithmException e) {
            throw new RuntimeException("Failed to hash request for idempotency", e);
        }
    }
}
