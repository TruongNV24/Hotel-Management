USE hotel_management;

ALTER TABLE maintenance_requests DROP CONSTRAINT chk_maintenance_type;

ALTER TABLE maintenance_requests
    ADD CONSTRAINT chk_maintenance_type
    CHECK (maintenance_type IN ('PREVENTIVE', 'CORRECTIVE', 'EMERGENCY', 'INSPECTION', 'CLEANING', 'OTHER'));