package com.qrorder.payment.api.dto;

import com.qrorder.payment.domain.PaymentProvider;
import jakarta.validation.constraints.NotNull;

public record CreatePaymentRequest(
        @NotNull(message = "Payment provider must not be null")
        PaymentProvider provider
) {}
