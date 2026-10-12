-- =============================================
-- V7: Audit log
-- (FR-A10)
-- =============================================

CREATE TABLE audit_log (
    id              BIGSERIAL       PRIMARY KEY,
    outlet_id       BIGINT,
    actor           VARCHAR(100)    NOT NULL,       -- username or 'SYSTEM'
    action          VARCHAR(50)     NOT NULL,       -- CREATE, UPDATE, DELETE
    entity_type     VARCHAR(50)     NOT NULL,       -- MenuItem, Order, etc.
    entity_id       VARCHAR(100)    NOT NULL,
    before_value    JSONB,
    after_value     JSONB,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_audit_log_entity ON audit_log(entity_type, entity_id);
CREATE INDEX idx_audit_log_outlet ON audit_log(outlet_id, created_at DESC);
