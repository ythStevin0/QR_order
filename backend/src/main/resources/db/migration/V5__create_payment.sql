-- =============================================
-- V5: Payment and idempotency
-- (FR-P1, FR-P5, FR-C9)
-- =============================================

CREATE TABLE payment (
    id              BIGSERIAL       PRIMARY KEY,
    order_id        UUID            NOT NULL REFERENCES orders(id),
    provider        VARCHAR(30)     NOT NULL,       -- MIDTRANS, XENDIT, MANUAL
    reference       VARCHAR(200)    NOT NULL UNIQUE, -- provider reference, prevents double webhook
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING',  -- PENDING, PAID, FAILED, EXPIRED, REFUNDED
    amount          BIGINT          NOT NULL,
    expires_at      TIMESTAMPTZ,
    raw_payload     JSONB,                          -- raw webhook/API response for audit
    paid_at         TIMESTAMPTZ,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_payment_order ON payment(order_id);

-- Idempotency keys — stored for 24 hours (FR-C9)
CREATE TABLE idempotency_key (
    key             VARCHAR(100)    PRIMARY KEY,
    request_hash    VARCHAR(64)     NOT NULL,       -- SHA-256 of request body
    response_body   JSONB           NOT NULL,
    response_status INTEGER         NOT NULL,
    expires_at      TIMESTAMPTZ     NOT NULL,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);
