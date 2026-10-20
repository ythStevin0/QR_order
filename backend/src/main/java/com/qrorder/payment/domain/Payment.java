package com.qrorder.payment.domain;

import com.qrorder.ordering.domain.Order;
import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

/**
 * Payment entity for tracking transactions and QRIS payments (FR-P1, FR-P5).
 * The 'reference' field is globally unique to ensure webhook idempotency.
 */
@Entity
@Table(name = "payment")
@Inheritance(strategy = InheritanceType.JOINED)
@DiscriminatorColumn(name = "payment_type", discriminatorType = DiscriminatorType.STRING)
public class Payment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentProvider provider;

    @Column(nullable = false, unique = true, length = 200)
    private String reference;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false)
    private long amount;

    @Column(name = "expires_at")
    private Instant expiresAt;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "raw_payload", columnDefinition = "jsonb")
    private String rawPayload;

    @Column(name = "paid_at")
    private Instant paidAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    protected Payment() {}

    public Payment(Order order, PaymentProvider provider, String reference, long amount, Instant expiresAt) {
        this.order = order;
        this.provider = provider;
        this.reference = reference;
        this.amount = amount;
        this.expiresAt = expiresAt;
        this.status = PaymentStatus.PENDING;
    }

    @PrePersist
    void onCreate() {
        createdAt = Instant.now();
        updatedAt = Instant.now();
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public void markPaid(String rawPayload) {
        this.status = PaymentStatus.PAID;
        this.paidAt = Instant.now();
        this.rawPayload = rawPayload;
    }

    public void markFailed(String rawPayload) {
        this.status = PaymentStatus.FAILED;
        this.rawPayload = rawPayload;
    }

    public void markExpired() {
        this.status = PaymentStatus.EXPIRED;
    }

    // --- Getters & Setters ---

    public Long getId() { return id; }
    public Order getOrder() { return order; }
    public PaymentProvider getProvider() { return provider; }
    public String getReference() { return reference; }
    public PaymentStatus getStatus() { return status; }
    public long getAmount() { return amount; }
    public Instant getExpiresAt() { return expiresAt; }
    public String getRawPayload() { return rawPayload; }
    public Instant getPaidAt() { return paidAt; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
