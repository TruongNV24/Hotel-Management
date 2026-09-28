USE hotel_management;

ALTER TABLE services
    ADD COLUMN IF NOT EXISTS created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    ADD COLUMN IF NOT EXISTS updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP;

ALTER TABLE service_usages
    ADD COLUMN IF NOT EXISTS total_amount DECIMAL(12,2) NOT NULL DEFAULT 0;

UPDATE service_usages
SET total_amount = quantity * unit_price
WHERE total_amount = 0 AND quantity > 0 AND unit_price > 0;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'services' AND index_name = 'idx_services_status') = 0,
    'CREATE INDEX idx_services_status ON services(status)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'service_usages' AND index_name = 'idx_service_usages_stay') = 0,
    'CREATE INDEX idx_service_usages_stay ON service_usages(stay_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @sql = IF(
    (SELECT COUNT(*) FROM information_schema.statistics
     WHERE table_schema = DATABASE() AND table_name = 'service_usages' AND index_name = 'idx_service_usages_service') = 0,
    'CREATE INDEX idx_service_usages_service ON service_usages(service_id)',
    'SELECT 1'
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
