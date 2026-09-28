USE hotel_management;

CREATE TABLE IF NOT EXISTS incidents (
    incident_id INT AUTO_INCREMENT PRIMARY KEY,
    incident_type VARCHAR(40) NOT NULL,
    title VARCHAR(200) NOT NULL,
    description TEXT,
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    status VARCHAR(20) NOT NULL DEFAULT 'OPEN',

    guest_id INT NULL,
    reservation_id INT NULL,
    stay_id INT NULL,
    invoice_id INT NULL,

    reported_by INT NOT NULL,
    assigned_to INT NULL,
    reported_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    resolved_at DATETIME NULL,
    closed_at DATETIME NULL,
    resolution TEXT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,

    CONSTRAINT chk_incident_type CHECK (incident_type IN ('PAYMENT', 'NO_CHECKOUT', 'PROPERTY_DAMAGE', 'GUEST_COMPLAINT', 'BILLING_DISPUTE', 'SERVICE_ISSUE', 'OTHER')),
    CONSTRAINT chk_incident_priority CHECK (priority IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT chk_incident_status CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED', 'CANCELLED')),
    CONSTRAINT fk_incident_guest FOREIGN KEY (guest_id) REFERENCES guests(guest_id) ON DELETE SET NULL,
    CONSTRAINT fk_incident_reservation FOREIGN KEY (reservation_id) REFERENCES reservations(reservation_id) ON DELETE SET NULL,
    CONSTRAINT fk_incident_stay FOREIGN KEY (stay_id) REFERENCES stays(stay_id) ON DELETE SET NULL,
    CONSTRAINT fk_incident_invoice FOREIGN KEY (invoice_id) REFERENCES invoices(invoice_id) ON DELETE SET NULL,
    CONSTRAINT fk_incident_reported_by FOREIGN KEY (reported_by) REFERENCES users(user_id),
    CONSTRAINT fk_incident_assigned_to FOREIGN KEY (assigned_to) REFERENCES users(user_id) ON DELETE SET NULL,
    INDEX idx_incidents_status (status),
    INDEX idx_incidents_type (incident_type),
    INDEX idx_incidents_priority (priority),
    INDEX idx_incidents_stay (stay_id),
    INDEX idx_incidents_reported_at (reported_at)
);
