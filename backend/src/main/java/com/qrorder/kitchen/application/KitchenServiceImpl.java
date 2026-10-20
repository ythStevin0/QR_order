package com.qrorder.kitchen.application;

import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import com.qrorder.ordering.domain.event.OrderPaidEvent;
import com.qrorder.ordering.domain.event.OrderStatusChangedEvent;
import com.qrorder.ordering.infrastructure.OrderRepository;
import com.qrorder.shared.error.ResourceNotFoundException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Service
public class KitchenServiceImpl implements KitchenService {

    private static final Logger log = LoggerFactory.getLogger(KitchenServiceImpl.class);
    private static final Long SSE_TIMEOUT = 300_000L; // 5 minutes

    private final OrderRepository orderRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final com.qrorder.shared.metrics.OrderMetricsService orderMetricsService;

    private final Map<Long, CopyOnWriteArrayList<SseEmitter>> kitchenEmitters = new ConcurrentHashMap<>();
    private final Map<UUID, CopyOnWriteArrayList<SseEmitter>> customerEmitters = new ConcurrentHashMap<>();

    public KitchenServiceImpl(OrderRepository orderRepository,
                              ApplicationEventPublisher eventPublisher,
                              com.qrorder.shared.metrics.OrderMetricsService orderMetricsService) {
        this.orderRepository = orderRepository;
        this.eventPublisher = eventPublisher;
        this.orderMetricsService = orderMetricsService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<OrderResponse> getActiveKitchenOrders(Long outletId) {
        log.info("Fetching active kitchen queue for outletId={}", outletId);
        List<OrderStatus> activeStatuses = List.of(OrderStatus.PAID, OrderStatus.PREPARING, OrderStatus.READY);
        List<Order> orders = orderRepository.findKitchenQueue(outletId, activeStatuses);
        return orders.stream()
                .map(OrderResponse::from)
                .toList();
    }

    @Override
    @Transactional
    public OrderResponse updateOrderStatus(UUID orderId, OrderStatus newStatus, String changedBy) {
        log.info("Updating order status [orderId={}, newStatus={}, changedBy={}]", orderId, newStatus, changedBy);

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        OrderStatus oldStatus = order.getStatus();
        order.transitionTo(newStatus, changedBy);
        Order savedOrder = orderRepository.save(order);

        // Record metrics for kitchen SLA
        if (newStatus == OrderStatus.READY && savedOrder.getCreatedAt() != null) {
            orderMetricsService.recordCookDuration(savedOrder.getOutlet().getId(),
                    java.time.Duration.between(savedOrder.getCreatedAt(), java.time.Instant.now()));
        } else if (newStatus == OrderStatus.COMPLETED) {
            orderMetricsService.recordOrderCompleted(savedOrder.getOutlet().getId());
        }

        // Publish event for SSE broadcast
        eventPublisher.publishEvent(new OrderStatusChangedEvent(
                savedOrder.getId(),
                savedOrder.getOutlet().getId(),
                oldStatus,
                newStatus,
                changedBy
        ));

        log.info("Order status successfully updated to {} [orderId={}]", newStatus, orderId);
        return OrderResponse.from(savedOrder);
    }

    @Override
    public SseEmitter registerKitchenStream(Long outletId) {
        log.info("Registering new SSE kitchen stream for outletId={}", outletId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        kitchenEmitters.computeIfAbsent(outletId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeKitchenEmitter(outletId, emitter));
        emitter.onTimeout(() -> removeKitchenEmitter(outletId, emitter));
        emitter.onError(e -> removeKitchenEmitter(outletId, emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to kitchen queue stream for outlet " + outletId));
        } catch (IOException e) {
            log.warn("Failed to send INIT event to kitchen stream", e);
            removeKitchenEmitter(outletId, emitter);
        }

        return emitter;
    }

    @Override
    public SseEmitter registerCustomerStream(UUID orderId) {
        log.info("Registering new SSE customer stream for orderId={}", orderId);
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        customerEmitters.computeIfAbsent(orderId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        emitter.onCompletion(() -> removeCustomerEmitter(orderId, emitter));
        emitter.onTimeout(() -> removeCustomerEmitter(orderId, emitter));
        emitter.onError(e -> removeCustomerEmitter(orderId, emitter));

        try {
            emitter.send(SseEmitter.event().name("INIT").data("Connected to order status stream for order " + orderId));
        } catch (IOException e) {
            log.warn("Failed to send INIT event to customer stream", e);
            removeCustomerEmitter(orderId, emitter);
        }

        return emitter;
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderPaid(OrderPaidEvent event) {
        log.info("Broadcasting OrderPaidEvent to kitchen [outletId={}, orderId={}]", event.outletId(), event.orderId());

        // Notify Kitchen Screen (bell ring 🔔 + new card)
        List<SseEmitter> emitters = kitchenEmitters.get(event.outletId());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("NEW_ORDER").data(event));
                } catch (IOException e) {
                    removeKitchenEmitter(event.outletId(), emitter);
                }
            }
        }

        // Notify Customer Screen
        List<SseEmitter> customerList = customerEmitters.get(event.orderId());
        if (customerList != null) {
            for (SseEmitter emitter : customerList) {
                try {
                    emitter.send(SseEmitter.event().name("STATUS_CHANGE").data(Map.of("status", "PAID")));
                } catch (IOException e) {
                    removeCustomerEmitter(event.orderId(), emitter);
                }
            }
        }
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onOrderStatusChanged(OrderStatusChangedEvent event) {
        log.info("Broadcasting OrderStatusChangedEvent [orderId={}, status={}]", event.orderId(), event.toStatus());

        // Notify Customer Screen
        List<SseEmitter> customerList = customerEmitters.get(event.orderId());
        if (customerList != null) {
            for (SseEmitter emitter : customerList) {
                try {
                    emitter.send(SseEmitter.event().name("STATUS_CHANGE").data(Map.of(
                            "orderId", event.orderId(),
                            "status", event.toStatus()
                    )));
                } catch (IOException e) {
                    removeCustomerEmitter(event.orderId(), emitter);
                }
            }
        }

        // Notify Kitchen Screen of update
        List<SseEmitter> emitters = kitchenEmitters.get(event.outletId());
        if (emitters != null) {
            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event().name("ORDER_UPDATED").data(event));
                } catch (IOException e) {
                    removeKitchenEmitter(event.outletId(), emitter);
                }
            }
        }
    }

    private void removeKitchenEmitter(Long outletId, SseEmitter emitter) {
        kitchenEmitters.computeIfPresent(outletId, (key, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }

    private void removeCustomerEmitter(UUID orderId, SseEmitter emitter) {
        customerEmitters.computeIfPresent(orderId, (key, list) -> {
            list.remove(emitter);
            return list.isEmpty() ? null : list;
        });
    }
}
