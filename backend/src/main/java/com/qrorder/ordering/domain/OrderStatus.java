package com.qrorder.ordering.domain;

/**
 * Order status following the state machine defined in PRD §8.5.
 *
 * Valid transitions:
 *   PENDING_PAYMENT → PAID (webhook only)
 *   PENDING_PAYMENT → CANCELLED (expiry or user cancel)
 *   PAID → PREPARING (kitchen starts)
 *   PAID → CANCELLED (staff cancel, triggers refund)
 *   PREPARING → READY (cooking done)
 *   READY → COMPLETED (delivered or picked up)
 */
public enum OrderStatus {

    PENDING_PAYMENT,
    PAID,
    PREPARING,
    READY,
    COMPLETED,
    CANCELLED;

    /**
     * Returns true if transitioning from this status to the target is valid.
     * PAID can only be set by the payment system, not staff — that rule is
     * enforced at the service layer, not here.
     */
    public boolean canTransitionTo(OrderStatus target) {
        return switch (this) {
            case PENDING_PAYMENT -> target == PAID || target == CANCELLED;
            case PAID -> target == PREPARING || target == CANCELLED;
            case PREPARING -> target == READY;
            case READY -> target == COMPLETED;
            case COMPLETED, CANCELLED -> false;  // terminal states
        };
    }

    /** Returns true if this is a terminal state (no further transitions). */
    public boolean isTerminal() {
        return this == COMPLETED || this == CANCELLED;
    }

    /** Returns true if this status means the order is active in the kitchen. */
    public boolean isActiveInKitchen() {
        return this == PAID || this == PREPARING || this == READY;
    }
}
