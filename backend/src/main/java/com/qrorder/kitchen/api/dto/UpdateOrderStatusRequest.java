package com.qrorder.kitchen.api.dto;

import com.qrorder.ordering.domain.OrderStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateOrderStatusRequest(
        @NotNull(message = "status must not be null")
        OrderStatus status
) {}
