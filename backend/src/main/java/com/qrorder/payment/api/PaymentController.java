package com.qrorder.payment.api;

import com.qrorder.payment.api.dto.CreatePaymentRequest;
import com.qrorder.payment.api.dto.PaymentResponse;
import com.qrorder.payment.api.dto.PaymentWebhookRequest;
import com.qrorder.payment.application.PaymentService;
import com.qrorder.shared.dto.WebResponse;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class PaymentController {

    private final PaymentService paymentService;

    public PaymentController(PaymentService paymentService) {
        this.paymentService = paymentService;
    }

    @PostMapping("/orders/{orderId}/payment")
    public ResponseEntity<WebResponse<PaymentResponse>> createPayment(
            @PathVariable UUID orderId,
            @RequestBody @Valid CreatePaymentRequest request
    ) {
        PaymentResponse response = paymentService.createPayment(orderId, request);
        return ResponseEntity.ok(WebResponse.success(response, "Payment QRIS generated successfully"));
    }

    @PostMapping("/payments/webhook")
    public ResponseEntity<WebResponse<String>> handleWebhook(
            @RequestBody @Valid PaymentWebhookRequest request
    ) {
        paymentService.processWebhook(request);
        return ResponseEntity.ok(WebResponse.success("Webhook processed successfully"));
    }
}
