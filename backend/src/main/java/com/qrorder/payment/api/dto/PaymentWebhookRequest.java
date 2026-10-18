package com.qrorder.payment.api.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PaymentWebhookRequest(
        @NotBlank(message = "reference must not be blank")
        String reference,

        @NotBlank(message = "transactionStatus must not be blank")
        String transactionStatus, // settlement, capture, expire, cancel, deny

        @JsonAlias("signature")
        String signatureKey
) {}
