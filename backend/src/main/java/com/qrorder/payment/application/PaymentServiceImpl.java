package com.qrorder.payment.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import com.qrorder.ordering.domain.event.OrderPaidEvent;
import com.qrorder.ordering.infrastructure.OrderRepository;
import com.qrorder.payment.api.dto.CreatePaymentRequest;
import com.qrorder.payment.api.dto.PaymentResponse;
import com.qrorder.payment.api.dto.PaymentWebhookRequest;
import com.qrorder.payment.domain.Payment;
import com.qrorder.payment.domain.PaymentStatus;
import com.qrorder.payment.infrastructure.PaymentRepository;
import com.qrorder.shared.error.BadRequestException;
import com.qrorder.shared.error.ResourceNotFoundException;
import com.qrorder.shared.outbox.OutboxEvent;
import com.qrorder.shared.outbox.OutboxEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Optional;
import java.util.UUID;

@Service
public class PaymentServiceImpl implements PaymentService {

    private static final Logger log = LoggerFactory.getLogger(PaymentServiceImpl.class);

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ApplicationEventPublisher eventPublisher;
    private final ObjectMapper objectMapper;

    public PaymentServiceImpl(PaymentRepository paymentRepository,
                              OrderRepository orderRepository,
                              OutboxEventRepository outboxEventRepository,
                              ApplicationEventPublisher eventPublisher,
                              ObjectMapper objectMapper) {
        this.paymentRepository = paymentRepository;
        this.orderRepository = orderRepository;
        this.outboxEventRepository = outboxEventRepository;
        this.eventPublisher = eventPublisher;
        this.objectMapper = objectMapper;
    }

    @Override
    @Transactional
    public PaymentResponse createPayment(UUID orderId, CreatePaymentRequest request) {
        log.info("Initiating payment request for orderId={} provider={}", orderId, request.provider());

        Order order = orderRepository.findById(orderId)
                .orElseThrow(() -> new ResourceNotFoundException("Order not found with id: " + orderId));

        if (order.getStatus() != OrderStatus.PENDING_PAYMENT) {
            throw new BadRequestException("Order is in %s status and cannot accept payment".formatted(order.getStatus()));
        }

        // Reuse existing pending payment if available and not expired
        Optional<Payment> existingPayment = paymentRepository.findByOrderId(orderId);
        if (existingPayment.isPresent()) {
            Payment p = existingPayment.get();
            if (p.getStatus() == PaymentStatus.PENDING && p.getExpiresAt() != null && Instant.now().isBefore(p.getExpiresAt())) {
                String qr = generateQrString(p.getReference());
                return PaymentResponse.from(p, qr);
            }
        }

        String reference = "QRIS-" + order.getId().toString().substring(0, 8) + "-" + System.currentTimeMillis();
        Instant expiresAt = Instant.now().plus(15, ChronoUnit.MINUTES);

        Payment payment = new Payment(order, request.provider(), reference, order.getTotal(), expiresAt);
        Payment savedPayment = paymentRepository.save(payment);

        String qrString = generateQrString(reference);
        log.info("QRIS generated successfully [orderId={}, reference={}, amount={}]", orderId, reference, order.getTotal());

        return PaymentResponse.from(savedPayment, qrString);
    }

    @Override
    @Transactional
    public void processWebhook(PaymentWebhookRequest webhookRequest) {
        log.info("Processing payment webhook callback [reference={}, status={}]",
                webhookRequest.reference(), webhookRequest.transactionStatus());

        Payment payment = paymentRepository.findByReference(webhookRequest.reference())
                .orElseThrow(() -> new ResourceNotFoundException("Payment reference not found: " + webhookRequest.reference()));

        // Idempotency: Ignore duplicate webhook if already marked PAID
        if (payment.getStatus() == PaymentStatus.PAID) {
            log.info("Duplicate webhook callback received for already PAID reference={}", webhookRequest.reference());
            return;
        }

        String status = webhookRequest.transactionStatus().toLowerCase();
        Order order = payment.getOrder();

        if (status.equals("settlement") || status.equals("capture") || status.equals("paid")) {
            payment.markPaid("{\"rawStatus\":\"" + status + "\"}");
            order.transitionTo(OrderStatus.PAID, "SYSTEM_WEBHOOK");

            paymentRepository.save(payment);
            orderRepository.save(order);

            // Record transactional outbox event
            OrderPaidEvent event = new OrderPaidEvent(
                    order.getId(),
                    order.getOutlet().getId(),
                    order.getTotal(),
                    order.getOrderType(),
                    order.getTableNumber(),
                    order.getQueueNumber()
            );

            try {
                String payload = objectMapper.writeValueAsString(event);
                OutboxEvent outbox = new OutboxEvent("OrderPaid", "Order", order.getId().toString(), order.getOutlet().getId(), payload);
                outboxEventRepository.save(outbox);
            } catch (JsonProcessingException e) {
                log.error("Failed to serialize outbox event payload", e);
            }

            // Publish Spring application event (listened by SSE kitchen stream)
            eventPublisher.publishEvent(event);
            log.info("Order status transitioned to PAID and event published [orderId={}]", order.getId());

        } else if (status.equals("expire") || status.equals("cancel") || status.equals("deny")) {
            payment.markExpired();
            order.transitionTo(OrderStatus.CANCELLED, "SYSTEM_TIMEOUT");
            paymentRepository.save(payment);
            orderRepository.save(order);
            log.warn("Payment expired/cancelled for orderId={} reference={}", order.getId(), webhookRequest.reference());
        }
    }

    private String generateQrString(String reference) {
        // EMVCo QRIS compliant mock prefix
        return "00020101021226680016ID.CO.QRIS.WWW01189360052300000000015204581253033605802ID5910QR_ORDER6007JAKARTA62"
                + String.format("%02d", reference.length()) + reference + "6304ABCD";
    }
}
