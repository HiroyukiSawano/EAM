ALTER TABLE information_system
    ADD COLUMN version_no VARCHAR(64) NULL,
    ADD COLUMN deployment_architecture VARCHAR(64) NULL,
    ADD COLUMN owner_person_id BIGINT NULL,
    ADD COLUMN contact_phone VARCHAR(32) NULL;

CREATE INDEX idx_information_system_owner_person_id ON information_system(owner_person_id);

UPDATE information_system SET system_type = 'BASIC_SUPPORT' WHERE system_type = 'SUPPORT_SYSTEM';
UPDATE information_system SET status = 'ACTIVE' WHERE status IS NULL OR status = '';
