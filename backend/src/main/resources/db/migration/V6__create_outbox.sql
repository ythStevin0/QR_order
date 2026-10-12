-- =============================================
-- V6: Outbox for async events
-- (FR-E9)
-- =============================================

CREATE TABLE outbox_event (
    id              BIGSERIAL       PRIMARY KEY,
    event_type      VARCHAR(50)     NOT NULL,       -- OrderPaid, OrderReady, MenuChanged, etc.
    aggregate_type  VARCHAR(50)     NOT NULL,        -- Order, MenuItem
    aggregate_id    VARCHAR(100)    NOT NULL,
    outlet_id       BIGINT          NOT NULL,
    payload         JSONB           NOT NULL,
    processed_at    TIMESTAMPTZ,                     -- NULL = not yet processed
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

-- Partial index: worker picks unprocessed events efficiently
CREATE INDEX idx_outbox_unprocessed ON outbox_event(created_at)
    WHERE processed_at IS NULL;
