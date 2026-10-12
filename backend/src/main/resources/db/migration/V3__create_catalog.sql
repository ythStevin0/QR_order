-- =============================================
-- V3: Catalog — menu items and dining tables
-- (FR-A1, FR-A3, FR-C3)
-- =============================================

CREATE TABLE menu_item (
    id          BIGSERIAL       PRIMARY KEY,
    outlet_id   BIGINT          NOT NULL REFERENCES outlet(id),
    name        VARCHAR(150)    NOT NULL,
    category    VARCHAR(50)     NOT NULL,
    description TEXT,
    price       BIGINT          NOT NULL,           -- in Rupiah (integer, never float)
    image_url   VARCHAR(500),
    is_available BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ     NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ     NOT NULL DEFAULT now()
);

CREATE INDEX idx_menu_item_outlet ON menu_item(outlet_id, is_available);

CREATE TABLE dining_table (
    id          BIGSERIAL   PRIMARY KEY,
    outlet_id   BIGINT      NOT NULL REFERENCES outlet(id),
    table_number VARCHAR(20) NOT NULL,
    is_active   BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT now(),

    UNIQUE (outlet_id, table_number)
);
