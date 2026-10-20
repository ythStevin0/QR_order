-- =============================================
-- V11: Polymorphic Payment Schema (InheritanceType.JOINED)
-- Supports QRIS, Cash (Kasir), and EDC Card Payments
-- =============================================

-- Add discriminator column to base payment table
ALTER TABLE payment ADD COLUMN IF NOT EXISTS payment_type VARCHAR(30) NOT NULL DEFAULT 'QRIS';

-- 1. Child Table: QRIS Payment Details
CREATE TABLE IF NOT EXISTS payment_qris (
    id              BIGINT          PRIMARY KEY REFERENCES payment(id) ON DELETE CASCADE,
    qr_string       TEXT,
    invoice_url     VARCHAR(500),
    nmid            VARCHAR(50),
    rrn             VARCHAR(50)
);

-- 2. Child Table: Cash / Kasir Payment Details
CREATE TABLE IF NOT EXISTS payment_cash (
    id              BIGINT          PRIMARY KEY REFERENCES payment(id) ON DELETE CASCADE,
    cashier_user_id BIGINT          REFERENCES staff_user(id),
    tendered_amount BIGINT          NOT NULL,
    change_amount   BIGINT          NOT NULL DEFAULT 0
);

-- 3. Child Table: EDC / Debit / Credit Card Payment Details
CREATE TABLE IF NOT EXISTS payment_edc (
    id                  BIGINT          PRIMARY KEY REFERENCES payment(id) ON DELETE CASCADE,
    bank_name           VARCHAR(50)     NOT NULL,
    approval_code       VARCHAR(50)     NOT NULL,
    masked_card_number  VARCHAR(20)     NOT NULL,
    card_type           VARCHAR(20)     NOT NULL -- DEBIT, CREDIT
);

-- Seed any existing payment into payment_qris for consistency
INSERT INTO payment_qris (id)
SELECT id FROM payment
ON CONFLICT (id) DO NOTHING;
