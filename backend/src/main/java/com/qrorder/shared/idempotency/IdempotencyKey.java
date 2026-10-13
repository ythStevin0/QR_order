package com.qrorder.shared.idempotency;

import jakarta.persistence.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.Instant;

/**
 * Stores idempotency keys to protect critical write operations (FR-C9).
 * E.g., multiple taps on 'Checkout' won't create duplicate orders.
 */
@Entity
@Table(name = "idempotency_key")
public class IdempotencyKey {

    @Id
    @Column(name = "key", length = 100)
    private String key;

    @Column(name = "request_hash", nullable = false, length = 64)
    private String requestHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "response_body", nullable = false, columnDefinition = "jsonb")
    private String responseBody;

    @Column(name = "response_status", nullable = false)
    private int responseStatus;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    protected IdempotencyKey() {}

    public IdempotencyKey(String key, String requestHash, String responseBody, int responseStatus, Instant expiresAt) {
        this.key = key;
        this.requestHash = requestHash;
        this.responseBody = responseBody;
        this.responseStatus = responseStatus;
        this.expiresAt = expiresAt;
    }

    @PrePersist
    void onCreate() {
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    public boolean isExpired() {
        return Instant.now().isAfter(expiresAt);
    }

    // --- Getters ---

    public String getKey() { return key; }
    public String getRequestHash() { return requestHash; }
    public String getResponseBody() { return responseBody; }
    public int getResponseStatus() { return responseStatus; }
    public Instant getExpiresAt() { return expiresAt; }
    public Instant getCreatedAt() { return createdAt; }
}
