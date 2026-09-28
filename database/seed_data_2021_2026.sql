USE hotel_management;

-- Module 6 deterministic synthetic seed. Existing rows are preserved.
-- Seed rows use SEED6 markers so this script can be rerun without duplicates.

DROP TEMPORARY TABLE IF EXISTS seed_numbers;
DROP TEMPORARY TABLE IF EXISTS seed_rooms;
DROP TEMPORARY TABLE IF EXISTS seed_services;
DROP TEMPORARY TABLE IF EXISTS seed_guests;
DROP TEMPORARY TABLE IF EXISTS seed_reservations;
DROP TEMPORARY TABLE IF EXISTS seed_stays;
DROP TEMPORARY TABLE IF EXISTS seed_invoices;

CREATE TEMPORARY TABLE seed_numbers (n INT PRIMARY KEY);
INSERT INTO seed_numbers (n)
WITH RECURSIVE sequence_numbers AS (
    SELECT 1 AS n
    UNION ALL
    SELECT n + 1 FROM sequence_numbers WHERE n < 240
)
SELECT n FROM sequence_numbers;

CREATE TEMPORARY TABLE seed_rooms AS
SELECT room_id, ROW_NUMBER() OVER (ORDER BY room_id) AS rn
FROM rooms;

CREATE TEMPORARY TABLE seed_services AS
SELECT service_id, ROW_NUMBER() OVER (ORDER BY service_id) AS rn
FROM services
WHERE status = 'ACTIVE';

START TRANSACTION;

-- 120 synthetic guests distributed over 2021-2026.
INSERT IGNORE INTO guests
(full_name, gender, date_of_birth, phone, email, id_card, address, nationality, created_at)
SELECT
    CONCAT(
        CASE MOD(n, 10)
            WHEN 0 THEN 'Nguyen An Minh'
            WHEN 1 THEN 'Tran Bao Chau'
            WHEN 2 THEN 'Le Gia Huy'
            WHEN 3 THEN 'Pham Khanh Linh'
            WHEN 4 THEN 'Hoang Duc Anh'
            WHEN 5 THEN 'Vo Thanh Ha'
            WHEN 6 THEN 'Bui Ngoc Lan'
            WHEN 7 THEN 'Dang Quoc Viet'
            WHEN 8 THEN 'Do Mai Phuong'
            ELSE 'Huynh Tuan Kiet'
        END,
        ' ', LPAD(n, 3, '0')
    ),
    CASE MOD(n, 3) WHEN 0 THEN 'MALE' WHEN 1 THEN 'FEMALE' ELSE 'OTHER' END,
    DATE_ADD('1980-01-01', INTERVAL MOD(n * 137, 11500) DAY),
    CONCAT('0906', LPAD(n, 6, '0')),
    CONCAT('guest', LPAD(n, 3, '0'), '@seed.hotel.test'),
    CONCAT('S6-', 2021 + FLOOR((n - 1) / 20), '-', LPAD(n, 4, '0')),
    CASE MOD(n, 6)
        WHEN 0 THEN 'Ha Noi'
        WHEN 1 THEN 'Hai Phong'
        WHEN 2 THEN 'Da Nang'
        WHEN 3 THEN 'Ho Chi Minh City'
        WHEN 4 THEN 'Can Tho'
        ELSE 'Nha Trang'
    END,
    CASE MOD(n, 4) WHEN 0 THEN 'Vietnam' WHEN 1 THEN 'Singapore' WHEN 2 THEN 'Japan' ELSE 'Australia' END,
    DATE_ADD(
        STR_TO_DATE(CONCAT(2021 + FLOOR((n - 1) / 20), '-01-01'), '%Y-%m-%d'),
        INTERVAL MOD(n - 1, 20) * 10 DAY
    )
FROM seed_numbers
WHERE n <= 120;

DROP TEMPORARY TABLE IF EXISTS seed_guests;
CREATE TEMPORARY TABLE seed_guests AS
SELECT guest_id, ROW_NUMBER() OVER (ORDER BY id_card) AS rn
FROM guests
WHERE id_card LIKE 'S6-%';

-- 120 non-overlapping historical reservations. Each room receives a 3-day stay
-- every 70 days or more, so generated reservations do not overlap each other.
INSERT IGNORE INTO reservations
(reservation_code, guest_id, room_id, check_in_date, check_out_date, number_of_guests, status, note, created_by, created_at)
SELECT
    CONCAT('SEED6-R-', LPAD(n.n, 4, '0')),
    g.guest_id,
    r.room_id,
    DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-01'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY),
    DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-01'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 + 3 DAY),
    1 + MOD(n.n, 3),
    'COMPLETED',
    'Synthetic historical reservation',
    (SELECT MIN(user_id) FROM users),
    DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-01'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY)
FROM seed_numbers n
JOIN seed_guests g ON g.rn = n.n
JOIN seed_rooms r ON r.rn = MOD(n.n - 1, (SELECT COUNT(*) FROM seed_rooms)) + 1
WHERE n.n <= 120;

DROP TEMPORARY TABLE IF EXISTS seed_reservations;
CREATE TEMPORARY TABLE seed_reservations AS
SELECT reservation_id, guest_id, room_id, check_in_date, check_out_date,
       ROW_NUMBER() OVER (ORDER BY reservation_code) AS rn
FROM reservations
WHERE reservation_code LIKE 'SEED6-R-%';

INSERT IGNORE INTO reservation_guests (reservation_id, guest_id, is_primary)
SELECT reservation_id, guest_id, TRUE
FROM seed_reservations;

-- Every seeded reservation has a completed stay with coherent timestamps.
INSERT IGNORE INTO stays
(reservation_id, room_id, actual_check_in, actual_check_out, status, check_in_by, check_out_by, note)
SELECT
    reservation_id,
    room_id,
    TIMESTAMP(check_in_date, '15:00:00'),
    TIMESTAMP(check_out_date, '11:00:00'),
    'CHECKED_OUT',
    (SELECT MIN(user_id) FROM users),
    (SELECT MIN(user_id) FROM users),
    'Synthetic historical stay'
FROM seed_reservations;

DROP TEMPORARY TABLE IF EXISTS seed_stays;
CREATE TEMPORARY TABLE seed_stays AS
SELECT st.stay_id, st.reservation_id, st.room_id, st.actual_check_in, st.actual_check_out,
       sr.guest_id, sr.check_in_date, sr.check_out_date,
       ROW_NUMBER() OVER (ORDER BY sr.rn) AS rn
FROM stays st
JOIN seed_reservations sr ON sr.reservation_id = st.reservation_id;

-- Two service usages per seeded stay. Prices are snapshots based on the
-- historical year and total_amount is always quantity * unit_price.
INSERT INTO service_usages
(stay_id, service_id, quantity, unit_price, total_amount, used_at, note, created_by)
SELECT x.stay_id, x.service_id, x.quantity, x.unit_price, x.quantity * x.unit_price,
       x.used_at, x.note, (SELECT MIN(user_id) FROM users)
FROM (
    SELECT
        st.stay_id,
        s.service_id,
        1 + MOD(num.n, 3) AS quantity,
        ROUND(s.price * CASE WHEN YEAR(st.actual_check_in) <= 2022 THEN 0.80 WHEN YEAR(st.actual_check_in) <= 2024 THEN 0.90 ELSE 1.00 END, 2) AS unit_price,
        DATE_ADD(st.actual_check_in, INTERVAL 1 + MOD(num.n, 5) HOUR) AS used_at,
        CONCAT('SEED6-USAGE-', LPAD(num.n, 4, '0')) AS note
    FROM seed_numbers num
    JOIN seed_stays st ON st.rn = MOD(num.n - 1, (SELECT COUNT(*) FROM seed_stays)) + 1
    JOIN seed_services ss ON ss.rn = MOD(num.n - 1, (SELECT COUNT(*) FROM seed_services)) + 1
    JOIN services s ON s.service_id = ss.service_id
    WHERE num.n <= 240
) x
WHERE NOT EXISTS (
    SELECT 1 FROM service_usages existing WHERE existing.note = x.note
);

-- One invoice per seeded stay. Invoice totals are derived from room charges
-- and the persisted service usage totals.
INSERT IGNORE INTO invoices
(invoice_code, stay_id, room_amount, service_amount, discount_amount, tax_amount, total_amount, status, issued_at, created_by)
SELECT
    CONCAT('SEED6-I-', LPAD(st.rn, 4, '0')),
    st.stay_id,
    ROUND(rt.price_per_night * DATEDIFF(st.check_out_date, st.check_in_date), 2),
    COALESCE(SUM(su.total_amount), 0),
    0,
    0,
    ROUND(rt.price_per_night * DATEDIFF(st.check_out_date, st.check_in_date) + COALESCE(SUM(su.total_amount), 0), 2),
    CASE WHEN st.rn <= 80 THEN 'PAID' WHEN st.rn <= 105 THEN 'PARTIALLY_PAID' ELSE 'UNPAID' END,
    DATE_ADD(st.actual_check_out, INTERVAL 2 HOUR),
    (SELECT MIN(user_id) FROM users)
FROM seed_stays st
JOIN rooms r ON r.room_id = st.room_id
JOIN room_types rt ON rt.room_type_id = r.room_type_id
LEFT JOIN service_usages su ON su.stay_id = st.stay_id AND su.note LIKE 'SEED6-USAGE-%'
GROUP BY st.rn, st.stay_id, rt.price_per_night, st.check_out_date, st.check_in_date, st.actual_check_out;

DROP TEMPORARY TABLE IF EXISTS seed_invoices;
CREATE TEMPORARY TABLE seed_invoices AS
SELECT i.invoice_id, i.stay_id, i.total_amount, i.status,
       ROW_NUMBER() OVER (ORDER BY i.invoice_code) AS rn
FROM invoices i
WHERE i.invoice_code LIKE 'SEED6-I-%';

-- Room and service invoice details are derived from the same source values.
INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount)
SELECT si.invoice_id, 'ROOM', CONCAT('Room charge - room ', r.room_number), DATEDIFF(sr.check_out_date, sr.check_in_date),
       rt.price_per_night, ROUND(rt.price_per_night * DATEDIFF(sr.check_out_date, sr.check_in_date), 2)
FROM seed_invoices si
JOIN stays st ON st.stay_id = si.stay_id
JOIN seed_reservations sr ON sr.reservation_id = st.reservation_id
JOIN rooms r ON r.room_id = st.room_id
JOIN room_types rt ON rt.room_type_id = r.room_type_id
WHERE NOT EXISTS (
    SELECT 1 FROM invoice_details d WHERE d.invoice_id = si.invoice_id AND d.item_type = 'ROOM'
);

INSERT INTO invoice_details (invoice_id, item_type, description, quantity, unit_price, amount)
SELECT si.invoice_id, 'SERVICE', CONCAT(s.service_name, ' usage ', su.usage_id), su.quantity, su.unit_price, su.total_amount
FROM seed_invoices si
JOIN service_usages su ON su.stay_id = si.stay_id AND su.note LIKE 'SEED6-USAGE-%'
JOIN services s ON s.service_id = su.service_id
WHERE NOT EXISTS (
    SELECT 1 FROM invoice_details d
    WHERE d.invoice_id = si.invoice_id AND d.item_type = 'SERVICE'
      AND d.description = CONCAT(s.service_name, ' usage ', su.usage_id)
);

-- Paid and partial invoices receive coherent payments. Unpaid invoices do not.
INSERT INTO payments (invoice_id, amount, payment_method, payment_date, note, received_by)
SELECT si.invoice_id,
       CASE WHEN si.status = 'PAID' THEN si.total_amount ELSE ROUND(si.total_amount * 0.50, 2) END,
       CASE MOD(si.rn, 3) WHEN 0 THEN 'CASH' WHEN 1 THEN 'CARD' ELSE 'BANK_TRANSFER' END,
       DATE_ADD(i.issued_at, INTERVAL 1 HOUR),
       CONCAT('SEED6-PAY-', LPAD(si.rn, 4, '0')),
       (SELECT MIN(user_id) FROM users)
FROM seed_invoices si
JOIN invoices i ON i.invoice_id = si.invoice_id
WHERE si.status IN ('PAID', 'PARTIALLY_PAID')
  AND NOT EXISTS (
      SELECT 1 FROM payments p WHERE p.note = CONCAT('SEED6-PAY-', LPAD(si.rn, 4, '0'))
  );

-- 120 incidents across the implemented incident categories and states.
INSERT INTO incidents
(incident_type, title, description, priority, status, guest_id, reservation_id, stay_id, invoice_id, reported_by, assigned_to, reported_at, resolved_at, closed_at, resolution, created_at, updated_at)
SELECT
    CASE MOD(n.n, 6) WHEN 0 THEN 'PAYMENT' WHEN 1 THEN 'NO_CHECKOUT' WHEN 2 THEN 'PROPERTY_DAMAGE' WHEN 3 THEN 'GUEST_COMPLAINT' WHEN 4 THEN 'BILLING_DISPUTE' ELSE 'SERVICE_ISSUE' END,
    CONCAT(
        CASE MOD(n.n, 6) WHEN 0 THEN 'Payment follow-up' WHEN 1 THEN 'Checkout follow-up' WHEN 2 THEN 'Room property inspection' WHEN 3 THEN 'Guest service complaint' WHEN 4 THEN 'Billing review' ELSE 'Service quality follow-up' END,
        ' - seed case ', LPAD(n.n, 3, '0')
    ),
    CONCAT('Synthetic operational history for reservation ', sr.reservation_id, ' and stay ', st.stay_id, '.'),
    CASE MOD(n.n, 4) WHEN 0 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' WHEN 2 THEN 'LOW' ELSE 'CRITICAL' END,
    CASE WHEN n.n <= 20 THEN 'OPEN' WHEN n.n <= 50 THEN 'IN_PROGRESS' WHEN n.n <= 100 THEN 'RESOLVED' WHEN n.n <= 115 THEN 'CLOSED' ELSE 'CANCELLED' END,
    sr.guest_id, sr.reservation_id, st.stay_id, si.invoice_id,
    (SELECT MIN(user_id) FROM users),
    CASE WHEN MOD(n.n, 2) = 0 THEN (SELECT MAX(user_id) FROM users) ELSE (SELECT MIN(user_id) FROM users) END,
    DATE_ADD(st.actual_check_in, INTERVAL 1 DAY),
    CASE WHEN n.n BETWEEN 51 AND 115 THEN DATE_ADD(st.actual_check_in, INTERVAL 2 DAY) ELSE NULL END,
    CASE WHEN n.n BETWEEN 101 AND 115 THEN DATE_ADD(st.actual_check_in, INTERVAL 3 DAY) ELSE NULL END,
    CASE WHEN n.n BETWEEN 51 AND 115 THEN 'Reviewed and recorded for historical operations.' ELSE NULL END,
    DATE_ADD(st.actual_check_in, INTERVAL 1 DAY),
    DATE_ADD(st.actual_check_in, INTERVAL 1 DAY)
FROM seed_numbers n
JOIN seed_stays st ON st.rn = MOD(n.n - 1, (SELECT COUNT(*) FROM seed_stays)) + 1
JOIN seed_reservations sr ON sr.reservation_id = st.reservation_id
JOIN seed_invoices si ON si.stay_id = st.stay_id
WHERE n.n <= 120
  AND NOT EXISTS (SELECT 1 FROM incidents i WHERE i.title = CONCAT(
        CASE MOD(n.n, 6) WHEN 0 THEN 'Payment follow-up' WHEN 1 THEN 'Checkout follow-up' WHEN 2 THEN 'Room property inspection' WHEN 3 THEN 'Guest service complaint' WHEN 4 THEN 'Billing review' ELSE 'Service quality follow-up' END,
        ' - seed case ', LPAD(n.n, 3, '0')));

    UPDATE incidents
    SET created_at = reported_at, updated_at = reported_at
    WHERE title LIKE '% - seed case %';

-- 120 maintenance history rows. The final row is an overdue IN_PROGRESS
-- record for the currently available room with the highest room_id.
INSERT INTO maintenance_requests
(room_id, title, description, maintenance_type, priority, status, reported_by, assigned_to, started_at, expected_end_at, completed_at, duration_minutes, resolution, notes, created_at, updated_at)
SELECT
    CASE WHEN n.n = 120 THEN (SELECT MAX(room_id) FROM rooms) ELSE r.room_id END,
    CONCAT(CASE MOD(n.n, 5) WHEN 0 THEN 'Air conditioner preventive service' WHEN 1 THEN 'Bathroom fixture repair' WHEN 2 THEN 'Electrical inspection' WHEN 3 THEN 'Furniture corrective repair' ELSE 'Room safety inspection' END, ' - seed ', LPAD(n.n, 3, '0')),
    CONCAT('Synthetic maintenance history for room ', CASE WHEN n.n = 120 THEN (SELECT MAX(room_number) FROM rooms) ELSE (SELECT room_number FROM rooms WHERE room_id = r.room_id) END, '.'),
    CASE MOD(n.n, 5) WHEN 0 THEN 'PREVENTIVE' WHEN 1 THEN 'CORRECTIVE' WHEN 2 THEN 'INSPECTION' WHEN 3 THEN 'EMERGENCY' ELSE 'OTHER' END,
    CASE MOD(n.n, 4) WHEN 0 THEN 'HIGH' WHEN 1 THEN 'MEDIUM' WHEN 2 THEN 'LOW' ELSE 'CRITICAL' END,
    CASE WHEN n.n <= 110 THEN 'COMPLETED' WHEN n.n <= 119 THEN 'CANCELLED' ELSE 'IN_PROGRESS' END,
    (SELECT MIN(user_id) FROM users),
    (SELECT MAX(user_id) FROM users),
    CASE WHEN n.n = 120 THEN '2026-09-20 08:00:00' ELSE DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-05'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY) + INTERVAL 8 HOUR END,
    CASE WHEN n.n = 120 THEN '2026-09-22 16:00:00' ELSE DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-05'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY) + INTERVAL 16 HOUR END,
    CASE WHEN n.n <= 110 THEN CASE WHEN n.n = 120 THEN NULL ELSE DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-05'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY) + INTERVAL 12 HOUR END ELSE NULL END,
    CASE WHEN n.n <= 110 THEN 240 ELSE NULL END,
    CASE WHEN n.n <= 110 THEN 'Completed during historical operations.' ELSE NULL END,
    CONCAT('SEED6-MAINT-', LPAD(n.n, 3, '0')),
    CASE WHEN n.n = 120 THEN '2026-09-19 08:00:00' ELSE DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-04'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY) + INTERVAL 8 HOUR END,
    CASE WHEN n.n = 120 THEN '2026-09-19 08:00:00' ELSE DATE_ADD(STR_TO_DATE(CONCAT(2021 + FLOOR((n.n - 1) / 20), '-01-04'), '%Y-%m-%d'), INTERVAL MOD(n.n - 1, 20) * 10 DAY) + INTERVAL 8 HOUR END
FROM seed_numbers n
JOIN seed_rooms r ON r.rn = MOD(n.n - 1, (SELECT COUNT(*) FROM seed_rooms)) + 1
WHERE n.n <= 120
  AND NOT EXISTS (SELECT 1 FROM maintenance_requests m WHERE m.notes = CONCAT('SEED6-MAINT-', LPAD(n.n, 3, '0')));

UPDATE maintenance_requests
SET created_at = CASE
        WHEN RIGHT(notes, 3) = '120' THEN '2026-09-19 08:00:00'
        ELSE DATE_ADD(
            STR_TO_DATE(CONCAT(2021 + FLOOR((CAST(RIGHT(notes, 3) AS UNSIGNED) - 1) / 20), '-01-04'), '%Y-%m-%d'),
            INTERVAL MOD(CAST(RIGHT(notes, 3) AS UNSIGNED) - 1, 20) * 10 DAY
        ) + INTERVAL 8 HOUR
    END,
    updated_at = created_at
WHERE notes LIKE 'SEED6-MAINT-%';

-- Keep the current seeded IN_PROGRESS maintenance visible in room status only
-- when the room remains available and has no active reservation.
UPDATE rooms r
SET r.status = 'MAINTENANCE'
WHERE r.room_id = (SELECT MAX(room_id) FROM rooms)
  AND r.status = 'AVAILABLE'
  AND NOT EXISTS (SELECT 1 FROM reservations res WHERE res.room_id = r.room_id AND res.status IN ('PENDING', 'CONFIRMED', 'CHECKED_IN'));

-- Historical audit records for the generated business activity.
INSERT INTO audit_logs
(action, module, entity_type, entity_id, actor_user_id, actor_username, actor_role, details, ip_address, status, created_at)
SELECT
    CASE MOD(n.n, 8) WHEN 0 THEN 'CREATE_RESERVATION' WHEN 1 THEN 'CHECK_IN' WHEN 2 THEN 'CHECK_OUT' WHEN 3 THEN 'CREATE_INVOICE' WHEN 4 THEN 'PAYMENT_CONFIRMED' WHEN 5 THEN 'CREATE_INCIDENT' WHEN 6 THEN 'CREATE_MAINTENANCE' ELSE 'COMPLETE_MAINTENANCE' END,
    CASE MOD(n.n, 8) WHEN 0 THEN 'RESERVATION' WHEN 1 THEN 'STAY' WHEN 2 THEN 'STAY' WHEN 3 THEN 'INVOICE' WHEN 4 THEN 'INVOICE' WHEN 5 THEN 'INCIDENT' ELSE 'MAINTENANCE' END,
    CASE MOD(n.n, 8) WHEN 0 THEN 'RESERVATION' WHEN 1 THEN 'STAY' WHEN 2 THEN 'STAY' WHEN 3 THEN 'INVOICE' WHEN 4 THEN 'PAYMENT' WHEN 5 THEN 'INCIDENT' ELSE 'MAINTENANCE' END,
    CASE MOD(n.n, 8) WHEN 0 THEN sr.reservation_id WHEN 1 THEN st.stay_id WHEN 2 THEN st.stay_id WHEN 3 THEN si.invoice_id WHEN 4 THEN si.invoice_id WHEN 5 THEN i.incident_id ELSE m.maintenance_id END,
    (SELECT MIN(user_id) FROM users),
    u.username, u.role,
    CONCAT('Synthetic historical audit event ', LPAD(n.n, 3, '0')),
    '127.0.0.1',
    'SUCCESS',
    DATE_ADD(st.actual_check_in, INTERVAL MOD(n.n, 6) HOUR)
FROM seed_numbers n
JOIN seed_stays st ON st.rn = MOD(n.n - 1, (SELECT COUNT(*) FROM seed_stays)) + 1
JOIN seed_reservations sr ON sr.reservation_id = st.reservation_id
JOIN seed_invoices si ON si.stay_id = st.stay_id
JOIN users u ON u.user_id = (SELECT MIN(user_id) FROM users)
LEFT JOIN incidents i ON i.title = CONCAT(
        CASE MOD(MOD(n.n, 120), 6) WHEN 0 THEN 'Payment follow-up' WHEN 1 THEN 'Checkout follow-up' WHEN 2 THEN 'Room property inspection' WHEN 3 THEN 'Guest service complaint' WHEN 4 THEN 'Billing review' ELSE 'Service quality follow-up' END,
        ' - seed case ', LPAD(MOD(n.n - 1, 120) + 1, 3, '0'))
LEFT JOIN maintenance_requests m ON m.notes = CONCAT('SEED6-MAINT-', LPAD(MOD(n.n - 1, 120) + 1, 3, '0'))
WHERE n.n <= 240
  AND NOT EXISTS (SELECT 1 FROM audit_logs a WHERE a.details = CONCAT('Synthetic historical audit event ', LPAD(n.n, 3, '0')));

COMMIT;

DROP TEMPORARY TABLE IF EXISTS seed_numbers;
DROP TEMPORARY TABLE IF EXISTS seed_rooms;
DROP TEMPORARY TABLE IF EXISTS seed_services;
DROP TEMPORARY TABLE IF EXISTS seed_guests;
DROP TEMPORARY TABLE IF EXISTS seed_reservations;
DROP TEMPORARY TABLE IF EXISTS seed_stays;
DROP TEMPORARY TABLE IF EXISTS seed_invoices;
