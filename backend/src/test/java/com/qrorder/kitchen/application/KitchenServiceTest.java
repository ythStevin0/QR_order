package com.qrorder.kitchen.application;

import com.qrorder.ordering.api.dto.OrderResponse;
import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import com.qrorder.ordering.domain.OrderType;
import com.qrorder.ordering.domain.event.OrderPaidEvent;
import com.qrorder.ordering.domain.event.OrderStatusChangedEvent;
import com.qrorder.ordering.infrastructure.OrderRepository;
import com.qrorder.outlet.domain.Outlet;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class KitchenServiceTest {

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @InjectMocks
    private KitchenServiceImpl kitchenService;

    private Outlet outlet;
    private Order order;

    @BeforeEach
    void setUp() {
        outlet = new Outlet("Outlet 1", "Jl. Test", "Asia/Jakarta");
        outlet.setId(1L);

        order = new Order(outlet, OrderType.DINE_IN, "05", null);
        order.addLine(1L, "Mie Gacoan", 15_000L, 1, null);
        order.calculateTotals(1000, 500);
        order.transitionTo(OrderStatus.PAID, "SYSTEM");
    }

    @Test
    @DisplayName("getActiveKitchenOrders: Fetches active queue (PAID, PREPARING, READY)")
    void getActiveKitchenOrdersSuccess() {
        when(orderRepository.findKitchenQueue(eq(1L), any())).thenReturn(List.of(order));

        List<OrderResponse> result = kitchenService.getActiveKitchenOrders(1L);

        assertEquals(1, result.size());
        assertEquals(order.getId(), result.get(0).id());
        assertEquals(OrderStatus.PAID, result.get(0).status());
    }

    @Test
    @DisplayName("updateOrderStatus: Successfully transitions PAID to PREPARING and publishes event")
    void updateOrderStatusSuccess() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(orderRepository.save(any(Order.class))).thenAnswer(invocation -> invocation.getArgument(0));

        OrderResponse response = kitchenService.updateOrderStatus(order.getId(), OrderStatus.PREPARING, "chef_andi");

        assertEquals(OrderStatus.PREPARING, response.status());
        verify(eventPublisher, times(1)).publishEvent(any(OrderStatusChangedEvent.class));
        verify(orderRepository, times(1)).save(order);
    }

    @Test
    @DisplayName("updateOrderStatus: Throws IllegalStateException when transition is invalid (PAID to READY directly)")
    void updateOrderStatusInvalidThrows() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(IllegalStateException.class, () ->
                kitchenService.updateOrderStatus(order.getId(), OrderStatus.READY, "chef_andi")
        );

        verify(orderRepository, never()).save(any());
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("SSE Stream: Successfully registers emitter and broadcasts OrderPaidEvent without error")
    void sseRegistrationAndBroadcast() {
        SseEmitter kitchenEmitter = kitchenService.registerKitchenStream(1L);
        assertNotNull(kitchenEmitter);

        OrderPaidEvent paidEvent = new OrderPaidEvent(order.getId(), 1L, 20_000L, OrderType.DINE_IN, "05", null);

        // Does not throw IOException
        assertDoesNotThrow(() -> kitchenService.onOrderPaid(paidEvent));
    }
}
