ALTER TABLE asset_hardware
    ADD COLUMN hardware_ip VARCHAR(64) NULL,
    ADD COLUMN hardware_model VARCHAR(128) NULL,
    ADD COLUMN hardware_brand VARCHAR(128) NULL,
    ADD COLUMN hardware_type VARCHAR(64) NULL,
    ADD COLUMN physical_location VARCHAR(255) NULL,
    ADD COLUMN network_environment VARCHAR(64) NULL,
    ADD COLUMN operating_system VARCHAR(128) NULL,
    ADD COLUMN purchase_date DATE NULL,
    ADD COLUMN owner_person_id BIGINT NULL,
    ADD COLUMN contact_phone VARCHAR(32) NULL;

CREATE INDEX idx_asset_hardware_owner_person_id ON asset_hardware(owner_person_id);
CREATE INDEX idx_asset_hardware_hardware_type ON asset_hardware(hardware_type);

UPDATE asset_hardware
SET hardware_ip = COALESCE(hardware_ip, management_ip, business_ip);

UPDATE asset_hardware
SET hardware_model = COALESCE(hardware_model, (
    SELECT detail.device_model
    FROM asset_hardware_query_terminal detail
    WHERE detail.hardware_asset_id = asset_hardware.id
    LIMIT 1
));

UPDATE asset_hardware
SET hardware_model = COALESCE(hardware_model, (
    SELECT detail.device_model
    FROM asset_hardware_ticket_terminal detail
    WHERE detail.hardware_asset_id = asset_hardware.id
    LIMIT 1
));

UPDATE asset_hardware
SET hardware_model = COALESCE(hardware_model, (
    SELECT detail.device_model
    FROM asset_hardware_self_service_terminal detail
    WHERE detail.hardware_asset_id = asset_hardware.id
    LIMIT 1
));

UPDATE asset_hardware
SET operating_system = COALESCE(operating_system, (
    SELECT detail.operating_system
    FROM asset_hardware_server detail
    WHERE detail.hardware_asset_id = asset_hardware.id
    LIMIT 1
));

UPDATE asset_hardware
SET physical_location = COALESCE(physical_location, (
    SELECT location.name
    FROM asset_location location
    WHERE location.id = asset_hardware.location_id
    LIMIT 1
));

UPDATE asset_hardware
SET owner_person_id = COALESCE(owner_person_id, (
    SELECT rel.person_id
    FROM asset_hardware_person_rel rel
    WHERE rel.hardware_asset_id = asset_hardware.id
      AND rel.relation_type = 'RESPONSIBLE'
    ORDER BY rel.id ASC
    LIMIT 1
));

UPDATE asset_hardware asset
LEFT JOIN person p ON p.id = asset.owner_person_id
SET asset.contact_phone = COALESCE(asset.contact_phone, p.mobile);

UPDATE asset_hardware
SET hardware_type = CASE
    WHEN hardware_category = 'SERVER' THEN 'SERVER'
    WHEN hardware_category = 'QUERY_TERMINAL' THEN 'TERMINAL_DEVICE'
    WHEN hardware_category = 'TICKET_TERMINAL' THEN 'TERMINAL_DEVICE'
    WHEN hardware_category = 'SELF_SERVICE_TERMINAL' THEN 'TERMINAL_DEVICE'
    ELSE COALESCE(hardware_type, 'PERIPHERAL')
END;

UPDATE asset_hardware
SET hardware_category = CASE
    WHEN hardware_type = 'SERVER' THEN 'SERVER'
    WHEN hardware_type = 'NETWORK_DEVICE' THEN 'NETWORK_DEVICE'
    WHEN hardware_type = 'TERMINAL_DEVICE' THEN 'TERMINAL_DEVICE'
    WHEN hardware_type = 'PERIPHERAL' THEN 'PERIPHERAL'
    ELSE hardware_category
END;

UPDATE asset_hardware
SET hardware_status = CASE
    WHEN hardware_status IN ('REGISTERED', 'IN_STOCK', 'ASSIGNED', 'CHANGED') THEN 'RUNNING'
    WHEN hardware_status = 'MAINTAINING' THEN 'MAINTENANCE'
    WHEN hardware_status IN ('IDLE', 'OFFLINE') THEN 'IDLE'
    WHEN hardware_status = 'SCRAPPED' THEN 'SCRAPPED'
    WHEN hardware_status IS NULL OR hardware_status = '' THEN 'RUNNING'
    ELSE hardware_status
END;

UPDATE asset_hardware
SET hardware_type = COALESCE(hardware_type, 'PERIPHERAL'),
    hardware_status = COALESCE(hardware_status, 'RUNNING');
