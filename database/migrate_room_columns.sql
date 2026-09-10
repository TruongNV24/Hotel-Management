USE hotel_management;

SET @add_image_path = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE rooms ADD COLUMN image_path JSON AFTER status',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rooms'
      AND column_name = 'image_path'
);
PREPARE stmt FROM @add_image_path;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SET @add_amenities = (
    SELECT IF(COUNT(*) = 0,
        'ALTER TABLE rooms ADD COLUMN amenities JSON AFTER image_path',
        'SELECT 1')
    FROM information_schema.columns
    WHERE table_schema = DATABASE()
      AND table_name = 'rooms'
      AND column_name = 'amenities'
);
PREPARE stmt FROM @add_amenities;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

SELECT column_name, data_type
FROM information_schema.columns
WHERE table_schema = DATABASE()
  AND table_name = 'rooms'
  AND column_name IN ('image_path', 'amenities');
