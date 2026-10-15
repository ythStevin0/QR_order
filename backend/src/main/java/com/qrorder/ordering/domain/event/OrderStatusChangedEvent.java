package com.qrorder.ordering.domain.event;

import com.qrorder.ordering.domain.OrderStatus;

import java.util.UUID;

public record OrderStatusChangedEvent(
        UUID orderId,
        Long outletId,
        OrderStatus fromStatus,
        OrderStatus toStatus,
        String changedBy
) {}
