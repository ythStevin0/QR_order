package com.qrorder.ordering.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrorder.catalog.domain.DiningTable;
import com.qrorder.catalog.domain.MenuItem;
import com.qrorder.catalog.infrastructure.DiningTableRepository;
import com.qrorder.catalog.infrastructure.MenuItemRepository;
import com.qrorder.ordering.api.dto.CreateOrderRequest;
import com.qrorder.ordering.api.dto.OrderItemRequest;
import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.ordering.domain.DailyCounter;
import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import com.qrorder.ordering.domain.OrderType;
import com.qrorder.ordering.infrastructure.DailyCounterRepository;
import com.qrorder.ordering.infrastructure.OrderRepository;
import com.qrorder.outlet.domain.Outlet;
import com.qrorder.outlet.infrastructure.OutletRepository;
import com.qrorder.shared.error.BadRequestException;
import com.qrorder.shared.error.IdempotencyConflictException;
import com.qrorder.shared.idempotency.IdempotencyKey;
import com.qrorder.shared.idempotency.IdempotencyKeyRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private MenuItemRepository menuItemRepository;

    @Mock
    private DiningTableRepository diningTableRepository;

    @Mock
    private OutletRepository outletRepository;

    @Mock
    private IdempotencyKeyRepository idempotencyKeyRepository;

    @Mock
    private com.qrorder.shared.metrics.OrderMetricsService orderMetricsService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private OrderServiceImpl orderService;

    private Outlet outlet;
    private MenuItem menuItem;
    private DiningTable table;

    @BeforeEach
    void setUp() {
        outlet = new Outlet("Outlet Utama", "Jl. Sudirman 10", "Asia/Jakarta");
        outlet.setId(1L);
        outlet.setTaxRate(1000);           // 10%
        outlet.setServiceChargeRate(500);  // 5%
        outlet.setOpen(true);

        menuItem = new MenuItem(outlet, "Mie Pedas", "Makanan", 20_000L);
        menuItem.setAvailable(true);

        table = new DiningTable(outlet, "10");
        table.setActive(true);
    }

    @Test
    @DisplayName("createOrder: Successfully creates DINE_IN order with server-calculated price")
    void createDineInOrderSuccess() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet));
        when(diningTableRepository.findByOutletIdAndTableNumber(1L, "10")).thenReturn(Optional.of(table));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                OrderType.DINE_IN,
                "10",
                null,
                "Level 2",
                List.of(new OrderItemRequest(1L, 2, "Level 2"))
        );

        OrderResponse response = orderService.createOrder(request, null);

        assertNotNull(response);
        assertEquals(OrderStatus.PENDING_PAYMENT, response.status());
        assertEquals("10", response.tableNumber());
        assertEquals(40_000L, response.subtotal());       // 2 * 20,000 = 40,000
        assertEquals(4_000L, response.taxAmount());       // 10% of 40,000 = 4,000
        assertEquals(2_000L, response.serviceCharge());   // 5% of 40,000 = 2,000
        assertEquals(46_000L, response.total());          // 40,000 + 4,000 + 2,000 = 46,000
        assertEquals(1, response.items().size());
        assertEquals(40_000L, response.items().get(0).subtotal());

        verify(orderRepository, times(1)).save(any(Order.class));
    }

    @Test
    @DisplayName("createOrder: Successfully creates TAKE_AWAY order (queue number assigned upon payment)")
    void createTakeAwayOrderSuccess() {
        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                OrderType.TAKE_AWAY,
                null,
                "Budi",
                "Tanpa seledri",
                List.of(new OrderItemRequest(1L, 1, null))
        );

        OrderResponse response = orderService.createOrder(request, null);

        assertNotNull(response);
        assertEquals(OrderType.TAKE_AWAY, response.orderType());
        assertEquals("Budi", response.customerName());
        assertNull(response.queueNumber(), "Queue number must be null until payment is confirmed");
        assertNull(response.tableNumber());
    }

    @Test
    @DisplayName("createOrder: Throws BadRequestException when menu item is unavailable/sold out")
    void createOrderUnavailableItemThrows() {
        menuItem.setAvailable(false); // Sold out

        when(outletRepository.findById(1L)).thenReturn(Optional.of(outlet));
        when(diningTableRepository.findByOutletIdAndTableNumber(1L, "10")).thenReturn(Optional.of(table));
        when(menuItemRepository.findById(1L)).thenReturn(Optional.of(menuItem));

        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                OrderType.DINE_IN,
                "10",
                null,
                null,
                List.of(new OrderItemRequest(1L, 1, null))
        );

        assertThrows(BadRequestException.class, () -> orderService.createOrder(request, null));
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: Idempotency returns cached response when same key and payload are submitted")
    void createOrderIdempotencyCached() throws Exception {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                OrderType.DINE_IN,
                "10",
                null,
                null,
                List.of(new OrderItemRequest(1L, 1, null))
        );

        Order order = new Order(outlet, OrderType.DINE_IN, "10", null);
        order.addLine(1L, "Mie Pedas", 20_000L, 1, null);
        order.calculateTotals(1000, 500);
        OrderResponse cachedResponse = OrderResponse.from(order);

        String json = objectMapper.writeValueAsString(cachedResponse);
        String requestJson = objectMapper.writeValueAsString(request);
        String hash = java.util.HexFormat.of().formatHex(
                java.security.MessageDigest.getInstance("SHA-256").digest(requestJson.getBytes(java.nio.charset.StandardCharsets.UTF_8))
        );

        IdempotencyKey keyRecord = new IdempotencyKey("idem-key-123", hash, json, 201, Instant.now().plusSeconds(3600));
        when(idempotencyKeyRepository.findById("idem-key-123")).thenReturn(Optional.of(keyRecord));

        OrderResponse result = orderService.createOrder(request, "idem-key-123");

        assertNotNull(result);
        assertEquals(order.getId(), result.id());
        verify(orderRepository, never()).save(any());
    }

    @Test
    @DisplayName("createOrder: Idempotency throws 422 conflict when payload differs for the same key")
    void createOrderIdempotencyConflictThrows() {
        CreateOrderRequest request = new CreateOrderRequest(
                1L,
                OrderType.DINE_IN,
                "10",
                null,
                null,
                List.of(new OrderItemRequest(1L, 1, null))
        );

        IdempotencyKey mismatchedKey = new IdempotencyKey("idem-key-123", "different-hash-value", "{}", 201, Instant.now().plusSeconds(3600));
        when(idempotencyKeyRepository.findById("idem-key-123")).thenReturn(Optional.of(mismatchedKey));

        assertThrows(IdempotencyConflictException.class, () -> orderService.createOrder(request, "idem-key-123"));
        verify(orderRepository, never()).save(any());
    }
}
