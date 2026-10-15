package com.qrorder.ordering.api.dto;

import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import com.qrorder.ordering.domain.OrderType;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record OrderResponse(
        UUID id,
        Long outletId,
        OrderType orderType,
        String tableNumber,
        Integer queueNumber,
        String customerName,
        long subtotal,
        long taxAmount,
        long serviceCharge,
        long total,
        OrderStatus status,
        String notes,
        int version,
        Instant createdAt,
        List<OrderLineResponse> items
) {
    public static OrderResponse from(Order order) {
        List<OrderLineResponse> itemResponses = order.getLines().stream()
                .map(OrderLineResponse::from)
                .toList();

        return new OrderResponse(
                order.getId(),
                order.getOutlet().getId(),
                order.getOrderType(),
                order.getTableNumber(),
                order.getQueueNumber(),
                order.getCustomerName(),
                order.getSubtotal(),
                order.getTaxAmount(),
                order.getServiceCharge(),
                order.getTotal(),
                order.getStatus(),
                order.getNotes(),
                order.getVersion(),
                order.getCreatedAt(),
                itemResponses
        );
    }
}
