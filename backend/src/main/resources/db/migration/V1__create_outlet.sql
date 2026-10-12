-- =============================================
-- V1: Outlet table
-- Foundation for multi-outlet support (FR-M1)
-- =============================================

CREATE TABLE outlet (
    id          BIGSERIAL    PRIMARY KEY,
    name        VARCHAR(100) NOT NULL,
    address     TEXT,
    timezone    VARCHAR(50)  NOT NULL DEFAULT 'Asia/Jakarta',
    tax_rate    INTEGER      NOT NULL DEFAULT 0,          -- basis points (110 = 1.10%)
    service_charge_rate INTEGER NOT NULL DEFAULT 0,       -- basis points
    is_open     BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

-- Seed a default outlet for development
INSERT INTO outlet (name, address, timezone, is_open)
VALUES ('Outlet Utama', 'Jl. Contoh No. 1', 'Asia/Jakarta', TRUE);
