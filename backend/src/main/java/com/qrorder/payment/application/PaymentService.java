package com.qrorder.payment.application;

import com.qrorder.payment.api.dto.CreatePaymentRequest;
import com.qrorder.payment.api.dto.PaymentResponse;
import com.qrorder.payment.api.dto.PaymentWebhookRequest;

import com.qrorder.payment.api.dto.PayCashRequest;
import com.qrorder.payment.api.dto.PayEdcRequest;

import java.util.UUID;

public interface PaymentService {
    PaymentResponse createPayment(UUID orderId, CreatePaymentRequest request);
    void processWebhook(PaymentWebhookRequest webhookRequest);
    PaymentResponse payWithCash(UUID orderId, PayCashRequest request);
    PaymentResponse payWithEdc(UUID orderId, PayEdcRequest request);
}
