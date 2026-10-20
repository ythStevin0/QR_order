package com.qrorder.payment;

import com.qrorder.ordering.domain.Order;
import com.qrorder.ordering.domain.OrderType;
import com.qrorder.outlet.domain.Outlet;
import com.qrorder.payment.domain.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;

class PaymentPolymorphismTest {

    private final Outlet testOutlet = new Outlet("Test Outlet", "Jakarta", "08123456789");
    private final Order testOrder = new Order(testOutlet, OrderType.DINE_IN, "12", "John");

    @Test
    @DisplayName("Polymorphic QrisPayment encapsulates QR details and inherits Payment")
    void testQrisPaymentInheritance() {
        QrisPayment qris = new QrisPayment(
                testOrder,
                PaymentProvider.MIDTRANS,
                "QRIS-REF-001",
                50000L,
                Instant.now().plusSeconds(900),
                "0002010102122659...",
                "https://simulator.midtrans.com/qris/123",
                "ID1020000000000"
        );

        assertThat(qris).isInstanceOf(Payment.class);
        assertThat(qris.getAmount()).isEqualTo(50000L);
        assertThat(qris.getStatus()).isEqualTo(PaymentStatus.PENDING);
        assertThat(qris.getQrString()).startsWith("0002010102122659");
        assertThat(qris.getNmid()).isEqualTo("ID1020000000000");
    }

    @Test
    @DisplayName("Polymorphic CashPayment encapsulates cashier and change details")
    void testCashPaymentInheritance() {
        CashPayment cash = new CashPayment(
                testOrder,
                PaymentProvider.MANUAL,
                "CASH-REF-002",
                45000L,
                101L,
                50000L,
                5000L
        );

        assertThat(cash).isInstanceOf(Payment.class);
        assertThat(cash.getAmount()).isEqualTo(45000L);
        assertThat(cash.getCashierUserId()).isEqualTo(101L);
        assertThat(cash.getTenderedAmount()).isEqualTo(50000L);
        assertThat(cash.getChangeAmount()).isEqualTo(5000L);
    }

    @Test
    @DisplayName("Polymorphic EdcCardPayment encapsulates bank approval code and card type")
    void testEdcCardPaymentInheritance() {
        EdcCardPayment edc = new EdcCardPayment(
                testOrder,
                PaymentProvider.MANUAL,
                "EDC-REF-003",
                120000L,
                "BCA",
                "APPR-8899",
                "5210-XXXX-XXXX-1234",
                "DEBIT"
        );

        assertThat(edc).isInstanceOf(Payment.class);
        assertThat(edc.getBankName()).isEqualTo("BCA");
        assertThat(edc.getApprovalCode()).isEqualTo("APPR-8899");
        assertThat(edc.getMaskedCardNumber()).isEqualTo("5210-XXXX-XXXX-1234");
        assertThat(edc.getCardType()).isEqualTo("DEBIT");
    }
}
