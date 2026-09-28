USE hotel_management;

CREATE TABLE IF NOT EXISTS audit_logs (
    audit_log_id INT AUTO_INCREMENT PRIMARY KEY,
    action VARCHAR(100) NOT NULL,
    module VARCHAR(100) NOT NULL DEFAULT 'SYSTEM',
    entity_type VARCHAR(100),
    entity_id INT,
    actor_user_id INT,
    actor_username VARCHAR(100),
    actor_role VARCHAR(30),
    details TEXT,
    ip_address VARCHAR(50),
    status ENUM('SUCCESS', 'FAILED', 'WARNING') NOT NULL DEFAULT 'SUCCESS',
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_audit_actor_user
        FOREIGN KEY (actor_user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL,

    INDEX idx_audit_action (action),
    INDEX idx_audit_module (module),
    INDEX idx_audit_actor (actor_user_id),
    INDEX idx_audit_created_at (created_at DESC)
);

INSERT INTO audit_logs (action, module, entity_type, entity_id, actor_user_id, actor_username, actor_role, details, ip_address, status)
VALUES
('LOGIN_SUCCESS', 'AUTH', 'USER', NULL, NULL, 'system', 'SYSTEM', 'Audit log initialized', '127.0.0.1', 'SUCCESS');
