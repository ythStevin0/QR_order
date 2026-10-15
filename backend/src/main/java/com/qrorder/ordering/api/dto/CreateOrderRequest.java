package com.qrorder.ordering.api.dto;

import com.qrorder.ordering.domain.OrderType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record CreateOrderRequest(
        @NotNull(message = "outletId must not be null")
        Long outletId,

        @NotNull(message = "orderType must not be null")
        OrderType orderType,

        String tableNumber,

        String customerName,

        String notes,

        @NotEmpty(message = "Order must contain at least one item")
        List<@Valid OrderItemRequest> items
) {}
