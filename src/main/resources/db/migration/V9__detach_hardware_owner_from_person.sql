ALTER TABLE asset_hardware
    ADD COLUMN owner_name VARCHAR(64) NULL;

UPDATE asset_hardware
SET owner_name = COALESCE(owner_name, (
    SELECT p.name
    FROM person p
    WHERE p.id = asset_hardware.owner_person_id
    LIMIT 1
));

UPDATE asset_hardware
SET owner_name = COALESCE(owner_name, (
    SELECT p.name
    FROM asset_hardware_person_rel rel
    LEFT JOIN person p ON p.id = rel.person_id
    WHERE rel.hardware_asset_id = asset_hardware.id
      AND rel.relation_type = 'RESPONSIBLE'
    ORDER BY rel.id ASC
    LIMIT 1
));

UPDATE asset_hardware
SET contact_phone = COALESCE(contact_phone, (
    SELECT p.mobile
    FROM asset_hardware_person_rel rel
    LEFT JOIN person p ON p.id = rel.person_id
    WHERE rel.hardware_asset_id = asset_hardware.id
      AND rel.relation_type = 'RESPONSIBLE'
    ORDER BY rel.id ASC
    LIMIT 1
));

UPDATE asset_hardware
SET owner_person_id = NULL
WHERE owner_person_id IS NOT NULL;

DELETE FROM asset_hardware_person_rel
WHERE relation_type = 'RESPONSIBLE';
