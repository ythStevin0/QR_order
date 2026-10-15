package com.qrorder.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

public record PaymentWebhookRequest(
        @NotBlank(message = "reference must not be blank")
        String reference,

        @NotBlank(message = "transactionStatus must not be blank")
        String transactionStatus, // settlement, capture, expire, cancel, deny

        String signatureKey
) {}
