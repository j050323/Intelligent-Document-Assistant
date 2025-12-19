-- Add fields for document operation logging
ALTER TABLE system_logs ADD COLUMN resource_id BIGINT;
ALTER TABLE system_logs ADD COLUMN resource_type VARCHAR(50);
ALTER TABLE system_logs ADD COLUMN operation_details TEXT;

-- Create indexes for better query performance
CREATE INDEX idx_system_logs_resource_type ON system_logs(resource_type);
CREATE INDEX idx_system_logs_resource_id ON system_logs(resource_id);
CREATE INDEX idx_system_logs_user_resource ON system_logs(user_id, resource_type);
CREATE INDEX idx_system_logs_operation_resource ON system_logs(operation_type, resource_type);
