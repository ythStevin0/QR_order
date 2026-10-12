-- =============================================
-- V2: Staff and role tables
-- RBAC per outlet (FR-K1, FR-M2)
-- =============================================

CREATE TABLE staff_user (
    id              BIGSERIAL       PRIMARY KEY,
    username        VARCHAR(50)     NOT NULL UNIQUE,
    password_hash   VARCHAR(255)    NOT NULL,
    display_name    VARCHAR(100)    NOT NULL,
    is_active       BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE TABLE staff_outlet_role (
    id          BIGSERIAL   PRIMARY KEY,
    staff_id    BIGINT      NOT NULL REFERENCES staff_user(id),
    outlet_id   BIGINT      NOT NULL REFERENCES outlet(id),
    role        VARCHAR(30) NOT NULL,  -- ADMIN, KITCHEN, CASHIER
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (staff_id, outlet_id, role)
);

CREATE INDEX idx_staff_outlet_role_staff ON staff_outlet_role(staff_id);
CREATE INDEX idx_staff_outlet_role_outlet ON staff_outlet_role(outlet_id);
