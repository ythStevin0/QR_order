package com.qrorder.payment.api.dto;

import com.qrorder.payment.domain.Payment;
import com.qrorder.payment.domain.PaymentProvider;
import com.qrorder.payment.domain.PaymentStatus;

import java.time.Instant;
import java.util.UUID;

public record PaymentResponse(
        Long id,
        UUID orderId,
        PaymentProvider provider,
        String reference,
        PaymentStatus status,
        long amount,
        String qrString,
        Instant expiresAt,
        Instant paidAt
) {
    public static PaymentResponse from(Payment payment, String qrString) {
        return new PaymentResponse(
                payment.getId(),
                payment.getOrder().getId(),
                payment.getProvider(),
                payment.getReference(),
                payment.getStatus(),
                payment.getAmount(),
                qrString,
                payment.getExpiresAt(),
                payment.getPaidAt()
        );
    }

    public static PaymentResponse from(Payment payment) {
        return from(payment, null);
    }
}
