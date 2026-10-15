package com.qrorder.ordering.domain.event;

import com.qrorder.ordering.domain.OrderType;

import java.util.UUID;

public record OrderPaidEvent(
        UUID orderId,
        Long outletId,
        long totalAmount,
        OrderType orderType,
        String tableNumber,
        Integer queueNumber
) {}
