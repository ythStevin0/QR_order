-- =============================================
-- V9: Seed initial dining tables and menu items for Outlet 1
-- =============================================

-- Update Outlet 1 default rates
UPDATE outlet
SET tax_rate = 1000, service_charge_rate = 500, is_open = TRUE
WHERE id = 1;

-- Seed Dining Tables (Meja 01 s/d Meja 20)
INSERT INTO dining_table (outlet_id, table_number, is_active) VALUES
(1, '01', TRUE),
(1, '02', TRUE),
(1, '03', TRUE),
(1, '04', TRUE),
(1, '05', TRUE),
(1, '06', TRUE),
(1, '07', TRUE),
(1, '08', TRUE),
(1, '09', TRUE),
(1, '10', TRUE),
(1, '11', TRUE),
(1, '12', TRUE),
(1, '13', TRUE),
(1, '14', TRUE),
(1, '15', TRUE),
(1, '16', TRUE),
(1, '17', TRUE),
(1, '18', TRUE),
(1, '19', TRUE),
(1, '20', TRUE)
ON CONFLICT (outlet_id, table_number) DO NOTHING;

-- Seed Initial Menu Catalog
INSERT INTO menu_item (outlet_id, name, category, description, price, is_available) VALUES
(1, 'Mie Hompimpa (Pedas Gurih)', 'Mie Pedas', 'Mie kenyal pedas asin gurih dengan topping ayam cincang dan pangsit krispi.', 15000, TRUE),
(1, 'Mie Suit (Original Gurih)', 'Mie Pedas', 'Mie original tanpa cabai dengan rasa asin gurih khas bumbu rahasia.', 15000, TRUE),
(1, 'Mie Gacoan (Pedas Manis)', 'Mie Pedas', 'Mie pedas manis dengan taburan ayam cincang dan kerupuk pangsit renyah.', 15500, TRUE),
(1, 'Pangsit Goreng (Isi 3)', 'Dimsum & Camilan', 'Pangsit renyah isi olahan daging ayam dan udang dengan saus asam manis.', 12000, TRUE),
(1, 'Udang Keju (Isi 3)', 'Dimsum & Camilan', 'Olahan udang dengan lelehan keju mozarella di dalam, dibalut tepung roti renyah.', 14500, TRUE),
(1, 'Siomay Ayam (Isi 3)', 'Dimsum & Camilan', 'Siomay kukus lembut daging ayam pilihan disajikan dengan saus bangkok.', 12500, TRUE),
(1, 'Es Gobak Sodor', 'Minuman Segar', 'Es manis segar dengan campuran buah cincau, jeli, dan susu kental manis.', 9500, TRUE),
(1, 'Es Petak Umpet', 'Minuman Segar', 'Minuman tropis buah belimbing dan jeruk nipis yang menyegarkan.', 9500, TRUE),
(1, 'Es Teh Manis', 'Minuman Segar', 'Teh melati seduh harum dengan es batu segar pelepas dahaga.', 5000, TRUE),
(1, 'Air Mineral Prima 600ml', 'Minuman Segar', 'Air mineral murni dingin dan higienis.', 4500, TRUE);
