package com.qrorder.payment.api.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Request payload for EDC Debit/Credit Card Payment (Topik 31: Payload validation).
 */
public record PayEdcRequest(
        @NotBlank(message = "Bank name is required")
        String bankName,

        @NotBlank(message = "Approval code is required")
        String approvalCode,

        @NotBlank(message = "Masked card number is required")
        String maskedCardNumber,

        @NotBlank(message = "Card type (DEBIT/CREDIT) is required")
        String cardType
) {}
