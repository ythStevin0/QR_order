package com.qrorder.payment.domain;

import com.qrorder.ordering.domain.Order;
import jakarta.persistence.Column;
import jakarta.persistence.DiscriminatorValue;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;

import java.time.Instant;

/**
 * Cash Payment handled by cashier staff (Polymorphic Child).
 */
@Entity
@Table(name = "payment_cash")
@DiscriminatorValue("CASH")
public class CashPayment extends Payment {

    @Column(name = "cashier_user_id")
    private Long cashierUserId;

    @Column(name = "tendered_amount", nullable = false)
    private long tenderedAmount;

    @Column(name = "change_amount", nullable = false)
    private long changeAmount;

    protected CashPayment() {
        super();
    }

    public CashPayment(Order order, PaymentProvider provider, String reference, long amount,
                       Long cashierUserId, long tenderedAmount, long changeAmount) {
        super(order, provider, reference, amount, Instant.now().plusSeconds(300));
        this.cashierUserId = cashierUserId;
        this.tenderedAmount = tenderedAmount;
        this.changeAmount = changeAmount;
    }

    public Long getCashierUserId() { return cashierUserId; }
    public void setCashierUserId(Long cashierUserId) { this.cashierUserId = cashierUserId; }

    public long getTenderedAmount() { return tenderedAmount; }
    public void setTenderedAmount(long tenderedAmount) { this.tenderedAmount = tenderedAmount; }

    public long getChangeAmount() { return changeAmount; }
    public void setChangeAmount(long changeAmount) { this.changeAmount = changeAmount; }
}
