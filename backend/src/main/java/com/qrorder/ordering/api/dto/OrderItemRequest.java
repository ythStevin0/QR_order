package com.qrorder.ordering.api.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record OrderItemRequest(
        @NotNull(message = "menuItemId must not be null")
        Long menuItemId,

        @Min(value = 1, message = "quantity must be at least 1")
        int quantity,

        String notes
) {}
