package com.qrorder.payment.api.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Request payload for Cashier Cash Payment (Topik 31: Payload validation).
 */
public record PayCashRequest(
        Long cashierUserId,
        @NotNull(message = "Tendered amount is required")
        @Positive(message = "Tendered amount must be greater than zero")
        Long tenderedAmount
) {}
