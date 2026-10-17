package com.qrorder.payment.application;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderStatus;
import com.qrorder.ordering.domain.OrderType;
import com.qrorder.ordering.domain.event.OrderPaidEvent;
import com.qrorder.ordering.infrastructure.OrderRepository;
import com.qrorder.outlet.domain.Outlet;
import com.qrorder.payment.api.dto.CreatePaymentRequest;
import com.qrorder.payment.api.dto.PaymentResponse;
import com.qrorder.payment.api.dto.PaymentWebhookRequest;
import com.qrorder.payment.domain.Payment;
import com.qrorder.payment.domain.PaymentProvider;
import com.qrorder.payment.domain.PaymentStatus;
import com.qrorder.payment.infrastructure.PaymentRepository;
import com.qrorder.shared.error.BadRequestException;
import com.qrorder.shared.outbox.OutboxEvent;
import com.qrorder.shared.outbox.OutboxEventRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PaymentServiceTest {

    @Mock
    private PaymentRepository paymentRepository;

    @Mock
    private OrderRepository orderRepository;

    @Mock
    private OutboxEventRepository outboxEventRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.qrorder.ordering.infrastructure.DailyCounterRepository dailyCounterRepository;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @InjectMocks
    private PaymentServiceImpl paymentService;

    private Outlet outlet;
    private Order order;

    @BeforeEach
    void setUp() {
        outlet = new Outlet("Outlet 1", "Jl. Test", "Asia/Jakarta");
        outlet.setId(1L);

        order = new Order(outlet, OrderType.DINE_IN, "12", null);
        order.addLine(1L, "Mie Gacoan", 15_000L, 2, null);
        order.calculateTotals(1000, 500);
    }

    @Test
    @DisplayName("createPayment: Successfully creates QRIS payment for pending order")
    void createPaymentSuccess() {
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));
        when(paymentRepository.findByOrderId(order.getId())).thenReturn(Optional.empty());
        when(paymentRepository.save(any(Payment.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PaymentResponse response = paymentService.createPayment(order.getId(), new CreatePaymentRequest(PaymentProvider.MIDTRANS));

        assertNotNull(response);
        assertEquals(order.getId(), response.orderId());
        assertEquals(PaymentProvider.MIDTRANS, response.provider());
        assertEquals(PaymentStatus.PENDING, response.status());
        assertNotNull(response.reference());
        assertNotNull(response.qrString());
        assertTrue(response.qrString().contains("ID.CO.QRIS"));

        verify(paymentRepository, times(1)).save(any(Payment.class));
    }

    @Test
    @DisplayName("createPayment: Throws BadRequestException when order is already PAID")
    void createPaymentAlreadyPaidThrows() {
        order.transitionTo(OrderStatus.PAID, "TEST");
        when(orderRepository.findById(order.getId())).thenReturn(Optional.of(order));

        assertThrows(BadRequestException.class, () ->
                paymentService.createPayment(order.getId(), new CreatePaymentRequest(PaymentProvider.MIDTRANS))
        );
        verify(paymentRepository, never()).save(any());
    }

    @Test
    @DisplayName("processWebhook: Successfully marks payment PAID, transitions order, saves outbox, and publishes event")
    void processWebhookSettlementSuccess() {
        Payment payment = new Payment(order, PaymentProvider.MIDTRANS, "REF-12345", order.getTotal(), null);
        when(paymentRepository.findByReference("REF-12345")).thenReturn(Optional.of(payment));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest("REF-12345", "settlement", "sig-abc");

        paymentService.processWebhook(webhookRequest);

        assertEquals(PaymentStatus.PAID, payment.getStatus());
        assertEquals(OrderStatus.PAID, order.getStatus());

        verify(paymentRepository, times(1)).save(payment);
        verify(orderRepository, times(1)).save(order);
        verify(outboxEventRepository, times(1)).save(any(OutboxEvent.class));
        verify(eventPublisher, times(1)).publishEvent(any(OrderPaidEvent.class));
    }

    @Test
    @DisplayName("processWebhook: Idempotent - ignores duplicate callback when already PAID")
    void processWebhookDuplicateIgnored() {
        Payment payment = new Payment(order, PaymentProvider.MIDTRANS, "REF-12345", order.getTotal(), null);
        payment.markPaid("{}");
        when(paymentRepository.findByReference("REF-12345")).thenReturn(Optional.of(payment));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest("REF-12345", "settlement", "sig-abc");

        paymentService.processWebhook(webhookRequest);

        // Should not transition or publish again
        verify(paymentRepository, never()).save(payment);
        verify(eventPublisher, never()).publishEvent(any());
    }

    @Test
    @DisplayName("processWebhook: Assigns queue number to TAKE_AWAY order upon payment settlement")
    void processWebhookTakeawayAssignsQueueNumber() {
        Order takeawayOrder = new Order(outlet, OrderType.TAKE_AWAY, null, "Siti");
        takeawayOrder.addLine(1L, "Mie Pedas", 15_000L, 1, null);
        takeawayOrder.calculateTotals(1000, 500);

        Payment payment = new Payment(takeawayOrder, PaymentProvider.MIDTRANS, "REF-TAKEAWAY-1", takeawayOrder.getTotal(), null);
        when(paymentRepository.findByReference("REF-TAKEAWAY-1")).thenReturn(Optional.of(payment));

        com.qrorder.ordering.domain.DailyCounter counter = new com.qrorder.ordering.domain.DailyCounter(outlet.getId(), java.time.LocalDate.now());
        when(dailyCounterRepository.findByOutletIdAndCounterDateWithLock(eq(outlet.getId()), any(java.time.LocalDate.class)))
                .thenReturn(Optional.of(counter));

        PaymentWebhookRequest webhookRequest = new PaymentWebhookRequest("REF-TAKEAWAY-1", "settlement", "sig-123");
        paymentService.processWebhook(webhookRequest);

        assertEquals(OrderStatus.PAID, takeawayOrder.getStatus());
        assertEquals(1, takeawayOrder.getQueueNumber());
        verify(dailyCounterRepository, times(1)).save(counter);
    }
}
