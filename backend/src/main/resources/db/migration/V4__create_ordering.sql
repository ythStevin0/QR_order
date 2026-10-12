-- =============================================
-- V4: Ordering — orders, lines, status history, daily counter
-- (FR-C7, FR-C11, FR-K4, FR-R1)
-- =============================================

CREATE TABLE orders (
    id              UUID            PRIMARY KEY,
    outlet_id       BIGINT          NOT NULL REFERENCES outlet(id),
    order_type      VARCHAR(20)       NOT NULL,  -- DINE_IN, TAKE_AWAY
    table_number    VARCHAR(20),               -- null for take-away
    queue_number    INTEGER,                   -- take-away queue, per outlet per day
    customer_name   VARCHAR(100),              -- required for take-away
    subtotal        BIGINT          NOT NULL DEFAULT 0,
    tax_amount      BIGINT          NOT NULL DEFAULT 0,
    service_charge  BIGINT          NOT NULL DEFAULT 0,
    total           BIGINT          NOT NULL DEFAULT 0,
    status          VARCHAR(30)     NOT NULL DEFAULT 'PENDING_PAYMENT',
    notes           TEXT,
    version         INTEGER         NOT NULL DEFAULT 0,  -- optimistic locking
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE order_line (
    id              BIGSERIAL   PRIMARY KEY,
    order_id        UUID        NOT NULL REFERENCES orders(id),
    menu_item_id    BIGINT      NOT NULL,          -- reference only; no FK to allow menu changes
    item_name       VARCHAR(150) NOT NULL,         -- snapshot at order time
    unit_price      BIGINT      NOT NULL,          -- snapshot at order time
    quantity        INTEGER     NOT NULL DEFAULT 1,
    notes           TEXT,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_line_order ON order_line(order_id);

CREATE TABLE order_status_history (
    id          BIGSERIAL       PRIMARY KEY,
    order_id    UUID            NOT NULL REFERENCES orders(id),
    from_status VARCHAR(30),
    to_status   VARCHAR(30)     NOT NULL,
    changed_by  VARCHAR(100),   -- staff username or 'SYSTEM'
    changed_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_order_status_history_order ON order_status_history(order_id, changed_at);

-- Daily counter for take-away queue numbers (per outlet per day)
CREATE TABLE daily_counter (
    outlet_id       BIGINT      NOT NULL REFERENCES outlet(id),
    counter_date    DATE        NOT NULL,
    takeaway_count  INTEGER     NOT NULL DEFAULT 0,

    PRIMARY KEY (outlet_id, counter_date)
);
