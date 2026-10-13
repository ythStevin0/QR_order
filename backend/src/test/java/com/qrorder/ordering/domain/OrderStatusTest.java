package com.qrorder.ordering.domain;

import com.qrorder.outlet.domain.Outlet;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class OrderStatusTest {

    @Test
    @DisplayName("State Machine: PENDING_PAYMENT can only transition to PAID or CANCELLED")
    void pendingPaymentTransitions() {
        assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PAID));
        assertTrue(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.CANCELLED));

        assertFalse(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.PREPARING));
        assertFalse(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.READY));
        assertFalse(OrderStatus.PENDING_PAYMENT.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("State Machine: PAID can transition to PREPARING or CANCELLED (refund)")
    void paidTransitions() {
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.PREPARING));
        assertTrue(OrderStatus.PAID.canTransitionTo(OrderStatus.CANCELLED));

        assertFalse(OrderStatus.PAID.canTransitionTo(OrderStatus.READY));
        assertFalse(OrderStatus.PAID.canTransitionTo(OrderStatus.COMPLETED));
    }

    @Test
    @DisplayName("State Machine: PREPARING can only transition to READY")
    void preparingTransitions() {
        assertTrue(OrderStatus.PREPARING.canTransitionTo(OrderStatus.READY));

        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.PAID));
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.COMPLETED));
        assertFalse(OrderStatus.PREPARING.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("State Machine: READY can only transition to COMPLETED")
    void readyTransitions() {
        assertTrue(OrderStatus.READY.canTransitionTo(OrderStatus.COMPLETED));

        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.PREPARING));
        assertFalse(OrderStatus.READY.canTransitionTo(OrderStatus.CANCELLED));
    }

    @Test
    @DisplayName("State Machine: Terminal states cannot transition to anything")
    void terminalStates() {
        assertTrue(OrderStatus.COMPLETED.isTerminal());
        assertTrue(OrderStatus.CANCELLED.isTerminal());

        for (OrderStatus target : OrderStatus.values()) {
            assertFalse(OrderStatus.COMPLETED.canTransitionTo(target));
            assertFalse(OrderStatus.CANCELLED.canTransitionTo(target));
        }
    }

    @Test
    @DisplayName("Order aggregate: Records status history on valid transition")
    void orderTransitionRecordsHistory() {
        Outlet outlet = new Outlet("Outlet 1", "Jl. Test", "Asia/Jakarta");
        Order order = new Order(outlet, OrderType.DINE_IN, "12", null);

        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatus());
        assertEquals(0, order.getStatusHistory().size());

        // Valid transition
        order.transitionTo(OrderStatus.PAID, "SYSTEM_WEBHOOK");
        assertEquals(OrderStatus.PAID, order.getStatus());
        assertEquals(1, order.getStatusHistory().size());
        assertEquals(OrderStatus.PENDING_PAYMENT, order.getStatusHistory().get(0).getFromStatus());
        assertEquals(OrderStatus.PAID, order.getStatusHistory().get(0).getToStatus());
        assertEquals("SYSTEM_WEBHOOK", order.getStatusHistory().get(0).getChangedBy());

        // Next transition
        order.transitionTo(OrderStatus.PREPARING, "kitchen_staff");
        assertEquals(OrderStatus.PREPARING, order.getStatus());
        assertEquals(2, order.getStatusHistory().size());
    }

    @Test
    @DisplayName("Order aggregate: Throws IllegalStateException on invalid transition")
    void orderInvalidTransitionThrows() {
        Outlet outlet = new Outlet("Outlet 1", "Jl. Test", "Asia/Jakarta");
        Order order = new Order(outlet, OrderType.DINE_IN, "12", null);

        // Cannot jump directly from PENDING_PAYMENT to READY
        IllegalStateException ex = assertThrows(IllegalStateException.class, () ->
                order.transitionTo(OrderStatus.READY, "staff")
        );
        assertTrue(ex.getMessage().contains("Cannot transition from PENDING_PAYMENT to READY"));
    }
}
