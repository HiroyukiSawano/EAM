ALTER TABLE project_info
    ADD COLUMN approval_batch_no VARCHAR(128) NULL,
    ADD COLUMN project_budget DECIMAL(12,2) NULL,
    ADD COLUMN contract_amount DECIMAL(12,2) NULL,
    ADD COLUMN owner_name VARCHAR(64) NULL,
    ADD COLUMN owner_phone VARCHAR(32) NULL,
    ADD COLUMN approval_date DATE NULL,
    ADD COLUMN initial_delivery_date DATE NULL,
    ADD COLUMN warranty_end_date DATE NULL,
    ADD COLUMN stage VARCHAR(64) NULL,
    ADD COLUMN payment_cycle_name VARCHAR(64) NULL,
    ADD COLUMN payment_ratio DECIMAL(7,2) NULL,
    ADD COLUMN payment_amount DECIMAL(12,2) NULL,
    ADD COLUMN planned_payment_date DATE NULL,
    ADD COLUMN actual_payment_date DATE NULL,
    ADD COLUMN payment_status VARCHAR(64) NULL;

CREATE TABLE project_document (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    file_name VARCHAR(128) NOT NULL,
    original_name VARCHAR(255) NOT NULL,
    file_size BIGINT NOT NULL,
    content_type VARCHAR(128) NOT NULL,
    file_url VARCHAR(255) NOT NULL,
    uploaded_at TIMESTAMP,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_document_project_id ON project_document(project_id);

UPDATE project_info SET project_type = 'SOFTWARE_UPGRADE' WHERE project_type = 'UPGRADE';
UPDATE project_info SET project_type = 'OPS_PROJECT' WHERE project_type = 'OPERATION';

UPDATE project_info SET project_status = 'CONSTRUCTION' WHERE project_status = 'BUILDING';
UPDATE project_info SET project_status = 'COMPLETED' WHERE project_status = 'FINISHED';
UPDATE project_info SET project_status = 'CONSTRUCTION' WHERE project_status = 'OPERATING';
