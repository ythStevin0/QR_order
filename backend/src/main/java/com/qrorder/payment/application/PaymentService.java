package com.qrorder.payment.application;

import com.qrorder.payment.api.dto.CreatePaymentRequest;
import com.qrorder.payment.api.dto.PaymentResponse;
import com.qrorder.payment.api.dto.PaymentWebhookRequest;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(UUID orderId, CreatePaymentRequest request);
    void processWebhook(PaymentWebhookRequest webhookRequest);
}
