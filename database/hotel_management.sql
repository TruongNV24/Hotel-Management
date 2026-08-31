-- DROP DATABASE IF EXISTS hotel_management;

CREATE DATABASE hotel_management
    CHARACTER SET utf8mb4
    COLLATE utf8mb4_unicode_ci;

USE hotel_management;


CREATE TABLE users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    role ENUM('ADMIN', 'MANAGER', 'RECEPTIONIST') NOT NULL DEFAULT 'RECEPTIONIST',
    phone VARCHAR(20),
    email VARCHAR(100),
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE room_types (
    room_type_id INT AUTO_INCREMENT PRIMARY KEY,
    type_name VARCHAR(50) NOT NULL UNIQUE,
    description VARCHAR(255),
    capacity INT NOT NULL,
    price_per_night DECIMAL(12,2) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',

    CONSTRAINT chk_room_type_capacity
        CHECK (capacity > 0),

    CONSTRAINT chk_room_type_price
        CHECK (price_per_night >= 0)
);

CREATE TABLE rooms (
    room_id INT AUTO_INCREMENT PRIMARY KEY,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    room_type_id INT NOT NULL,
    floor INT NOT NULL,
    status ENUM(
        'AVAILABLE',
        'OCCUPIED',
        'RESERVED',
        'CLEANING',
        'MAINTENANCE'
    ) NOT NULL DEFAULT 'AVAILABLE',
    note VARCHAR(255),

    CONSTRAINT fk_rooms_room_type
        FOREIGN KEY (room_type_id)
        REFERENCES room_types(room_type_id),

    CONSTRAINT chk_room_floor
        CHECK (floor > 0)
);

CREATE TABLE guests (
    guest_id INT AUTO_INCREMENT PRIMARY KEY,
    full_name VARCHAR(100) NOT NULL,
    gender ENUM('MALE', 'FEMALE', 'OTHER'),
    date_of_birth DATE,
    phone VARCHAR(20) NOT NULL,
    email VARCHAR(100),
    id_card VARCHAR(30) NOT NULL UNIQUE,
    address VARCHAR(255),
    nationality VARCHAR(50) DEFAULT 'Vietnam',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE reservations (
    reservation_id INT AUTO_INCREMENT PRIMARY KEY,
    reservation_code VARCHAR(20) NOT NULL UNIQUE,

    guest_id INT NOT NULL,
    room_id INT NOT NULL,

    check_in_date DATE NOT NULL,
    check_out_date DATE NOT NULL,

    number_of_guests INT NOT NULL DEFAULT 1,

    status ENUM(
        'PENDING',
        'CONFIRMED',
        'CHECKED_IN',
        'COMPLETED',
        'CANCELLED'
    ) NOT NULL DEFAULT 'PENDING',

    note VARCHAR(255),

    created_by INT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_reservation_guest
        FOREIGN KEY (guest_id)
        REFERENCES guests(guest_id),

    CONSTRAINT fk_reservation_room
        FOREIGN KEY (room_id)
        REFERENCES rooms(room_id),

    CONSTRAINT fk_reservation_user
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_reservation_dates
        CHECK (check_out_date > check_in_date),

    CONSTRAINT chk_reservation_guests
        CHECK (number_of_guests > 0)
);

CREATE TABLE reservation_guests (
    reservation_id INT NOT NULL,
    guest_id INT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,

    PRIMARY KEY (reservation_id, guest_id),

    CONSTRAINT fk_rg_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(reservation_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_rg_guest
        FOREIGN KEY (guest_id)
        REFERENCES guests(guest_id)
        ON DELETE CASCADE
);

CREATE TABLE stays (
    stay_id INT AUTO_INCREMENT PRIMARY KEY,

    reservation_id INT NOT NULL UNIQUE,
    room_id INT NOT NULL,

    actual_check_in DATETIME,
    actual_check_out DATETIME,

    status ENUM(
        'CHECKED_IN',
        'CHECKED_OUT'
    ) NOT NULL DEFAULT 'CHECKED_IN',

    check_in_by INT,
    check_out_by INT,

    note VARCHAR(255),

    CONSTRAINT fk_stay_reservation
        FOREIGN KEY (reservation_id)
        REFERENCES reservations(reservation_id),

    CONSTRAINT fk_stay_room
        FOREIGN KEY (room_id)
        REFERENCES rooms(room_id),

    CONSTRAINT fk_stay_checkin_user
        FOREIGN KEY (check_in_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT fk_stay_checkout_user
        FOREIGN KEY (check_out_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL
);

CREATE TABLE services (
    service_id INT AUTO_INCREMENT PRIMARY KEY,
    service_name VARCHAR(100) NOT NULL UNIQUE,
    unit VARCHAR(30) NOT NULL,
    price DECIMAL(12,2) NOT NULL,
    status ENUM('ACTIVE', 'INACTIVE') NOT NULL DEFAULT 'ACTIVE',
    description VARCHAR(255),

    CONSTRAINT chk_service_price
        CHECK (price >= 0)
);

CREATE TABLE service_usages (
    usage_id INT AUTO_INCREMENT PRIMARY KEY,

    stay_id INT NOT NULL,
    service_id INT NOT NULL,

    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,

    used_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    note VARCHAR(255),

    created_by INT,

    CONSTRAINT fk_usage_stay
        FOREIGN KEY (stay_id)
        REFERENCES stays(stay_id)
        ON DELETE CASCADE,

    CONSTRAINT fk_usage_service
        FOREIGN KEY (service_id)
        REFERENCES services(service_id),

    CONSTRAINT fk_usage_user
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_usage_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_usage_unit_price
        CHECK (unit_price >= 0)
);

CREATE TABLE invoices (
    invoice_id INT AUTO_INCREMENT PRIMARY KEY,

    invoice_code VARCHAR(20) NOT NULL UNIQUE,

    stay_id INT NOT NULL UNIQUE,

    room_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    service_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    discount_amount DECIMAL(12,2) NOT NULL DEFAULT 0,
    tax_amount DECIMAL(12,2) NOT NULL DEFAULT 0,

    total_amount DECIMAL(12,2) NOT NULL DEFAULT 0,

    status ENUM(
        'UNPAID',
        'PARTIALLY_PAID',
        'PAID',
        'CANCELLED'
    ) NOT NULL DEFAULT 'UNPAID',

    issued_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    created_by INT,

    CONSTRAINT fk_invoice_stay
        FOREIGN KEY (stay_id)
        REFERENCES stays(stay_id),

    CONSTRAINT fk_invoice_user
        FOREIGN KEY (created_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_invoice_amount
        CHECK (
            room_amount >= 0
            AND service_amount >= 0
            AND discount_amount >= 0
            AND tax_amount >= 0
            AND total_amount >= 0
        )
);

CREATE TABLE invoice_details (
    invoice_detail_id INT AUTO_INCREMENT PRIMARY KEY,

    invoice_id INT NOT NULL,

    item_type ENUM(
        'ROOM',
        'SERVICE'
    ) NOT NULL,

    description VARCHAR(255) NOT NULL,

    quantity INT NOT NULL DEFAULT 1,
    unit_price DECIMAL(12,2) NOT NULL,
    amount DECIMAL(12,2) NOT NULL,

    CONSTRAINT fk_invoice_detail_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(invoice_id)
        ON DELETE CASCADE,

    CONSTRAINT chk_invoice_detail_quantity
        CHECK (quantity > 0),

    CONSTRAINT chk_invoice_detail_price
        CHECK (unit_price >= 0),

    CONSTRAINT chk_invoice_detail_amount
        CHECK (amount >= 0)
);

CREATE TABLE payments (
    payment_id INT AUTO_INCREMENT PRIMARY KEY,

    invoice_id INT NOT NULL,

    amount DECIMAL(12,2) NOT NULL,

    payment_method ENUM(
        'CASH',
        'BANK_TRANSFER',
        'CARD'
    ) NOT NULL DEFAULT 'CASH',

    payment_date DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    note VARCHAR(255),

    received_by INT,

    CONSTRAINT fk_payment_invoice
        FOREIGN KEY (invoice_id)
        REFERENCES invoices(invoice_id),

    CONSTRAINT fk_payment_user
        FOREIGN KEY (received_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    CONSTRAINT chk_payment_amount
        CHECK (amount > 0)
);

CREATE INDEX idx_rooms_status
ON rooms(status);

CREATE INDEX idx_reservations_guest
ON reservations(guest_id);

CREATE INDEX idx_reservations_room
ON reservations(room_id);

CREATE INDEX idx_reservations_dates
ON reservations(check_in_date, check_out_date);

CREATE INDEX idx_stays_room
ON stays(room_id);

CREATE INDEX idx_service_usage_stay
ON service_usages(stay_id);

CREATE INDEX idx_payments_invoice
ON payments(invoice_id);


-- sample data
-- USERS
INSERT INTO users
(username, password, full_name, role, phone, email)
VALUES
('admin', '123456', 'Quản trị viên', 'ADMIN',
 '0900000001', 'admin@hotel.com'),

('manager', '123456', 'Nguyễn Văn Quản', 'MANAGER',
 '0900000002', 'manager@hotel.com'),

('reception', '123456', 'Trần Thị Lễ Tân', 'RECEPTIONIST',
 '0900000003', 'reception@hotel.com');


-- ROOM TYPES
INSERT INTO room_types
(type_name, description, capacity, price_per_night)
VALUES
('Single', 'Phòng đơn tiêu chuẩn', 1, 500000),
('Double', 'Phòng đôi tiêu chuẩn', 2, 800000),
('Deluxe', 'Phòng Deluxe cao cấp', 2, 1200000),
('Family', 'Phòng gia đình', 4, 1800000);


-- ROOMS
INSERT INTO rooms
(room_number, room_type_id, floor, status, note)
VALUES
('101', 1, 1, 'AVAILABLE', NULL),
('102', 1, 1, 'AVAILABLE', NULL),
('103', 2, 1, 'AVAILABLE', NULL),
('104', 2, 1, 'AVAILABLE', NULL),

('201', 2, 2, 'AVAILABLE', NULL),
('202', 2, 2, 'AVAILABLE', NULL),
('203', 3, 2, 'AVAILABLE', NULL),
('204', 3, 2, 'AVAILABLE', NULL),

('301', 4, 3, 'AVAILABLE', NULL),
('302', 4, 3, 'AVAILABLE', NULL);


-- SERVICES
INSERT INTO services
(service_name, unit, price, description)
VALUES
('Nước suối', 'Chai', 15000, 'Nước suối đóng chai'),
('Nước ngọt', 'Lon', 20000, 'Nước ngọt'),
('Bữa sáng', 'Suất', 80000, 'Bữa sáng tại khách sạn'),
('Giặt ủi', 'Kg', 50000, 'Dịch vụ giặt ủi'),
('Thuê xe', 'Ngày', 300000, 'Dịch vụ thuê xe');


-- GUESTS
INSERT INTO guests
(full_name, gender, date_of_birth, phone, email, id_card, address, nationality)
VALUES
('Nguyễn Văn An', 'MALE', '2000-05-15',
 '0911111111', 'an@gmail.com', '001200000001',
 'Hà Nội', 'Vietnam'),

('Trần Thị Bình', 'FEMALE', '1999-08-20',
 '0922222222', 'binh@gmail.com', '001200000002',
 'Hà Nội', 'Vietnam'),

('Lê Văn Cường', 'MALE', '1998-03-10',
 '0933333333', 'cuong@gmail.com', '001200000003',
 'Hải Phòng', 'Vietnam');


-- test reservation

INSERT INTO reservations
(
    reservation_code,
    guest_id,
    room_id,
    check_in_date,
    check_out_date,
    number_of_guests,
    status,
    note,
    created_by
)
VALUES
(
    'RES0001',
    1,
    3,
    '2026-09-05',
    '2026-09-07',
    2,
    'CONFIRMED',
    'Khách đặt phòng trước',
    3
);


-- Reservation guests
INSERT INTO reservation_guests
(reservation_id, guest_id, is_primary)
VALUES
(1, 1, TRUE),
(1, 2, FALSE);



CREATE VIEW v_room_list AS
SELECT
    r.room_id,
    r.room_number,
    rt.type_name,
    rt.capacity,
    rt.price_per_night,
    r.floor,
    r.status,
    r.note
FROM rooms r
JOIN room_types rt
    ON r.room_type_id = rt.room_type_id;


CREATE VIEW v_reservation_list AS
SELECT
    res.reservation_id,
    res.reservation_code,
    g.full_name AS guest_name,
    g.phone,
    r.room_number,
    rt.type_name,
    res.check_in_date,
    res.check_out_date,
    res.number_of_guests,
    res.status,
    u.full_name AS created_by,
    res.created_at
FROM reservations res

JOIN guests g
    ON res.guest_id = g.guest_id

JOIN rooms r
    ON res.room_id = r.room_id

JOIN room_types rt
    ON r.room_type_id = rt.room_type_id

LEFT JOIN users u
    ON res.created_by = u.user_id;

CREATE VIEW v_service_usage AS
SELECT
    su.usage_id,
    su.stay_id,
    g.full_name AS guest_name,
    r.room_number,
    s.service_name,
    su.quantity,
    su.unit_price,
    su.quantity * su.unit_price AS amount,
    su.used_at
FROM service_usages su

JOIN stays st
    ON su.stay_id = st.stay_id

JOIN reservations res
    ON st.reservation_id = res.reservation_id

JOIN guests g
    ON res.guest_id = g.guest_id

JOIN rooms r
    ON st.room_id = r.room_id

JOIN services s
    ON su.service_id = s.service_id;

