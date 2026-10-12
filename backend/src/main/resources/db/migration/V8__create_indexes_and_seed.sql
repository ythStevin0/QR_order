-- =============================================
-- V8: Critical indexes (FR-E3)
-- =============================================

-- Kitchen queue: fetch active orders for an outlet, ordered by creation time
CREATE INDEX idx_orders_kitchen_queue ON orders(outlet_id, status, created_at);

-- Order history: keyset pagination (outlet_id, created_at DESC, id DESC)
CREATE INDEX idx_orders_history ON orders(outlet_id, created_at DESC, id DESC);

-- Payment reference is already UNIQUE in V5 (serves as the webhook dedup index)
-- daily_counter PK (outlet_id, counter_date) is already created in V4

-- Seed dev data: default admin user (password: admin123, BCrypt)
INSERT INTO staff_user (username, password_hash, display_name)
VALUES ('admin', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Administrator');

INSERT INTO staff_outlet_role (staff_id, outlet_id, role)
VALUES (1, 1, 'ADMIN');

INSERT INTO staff_user (username, password_hash, display_name)
VALUES ('kitchen1', '$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy', 'Staf Dapur 1');

INSERT INTO staff_outlet_role (staff_id, outlet_id, role)
VALUES (2, 1, 'KITCHEN');
