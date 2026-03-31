ALTER TABLE service_provider ADD COLUMN short_name VARCHAR(128);
ALTER TABLE service_provider ADD COLUMN logo_url VARCHAR(255);
ALTER TABLE service_provider ADD COLUMN enterprise_nature VARCHAR(64);
ALTER TABLE service_provider ADD COLUMN vendor_level VARCHAR(64);
ALTER TABLE service_provider ADD COLUMN score INT;
ALTER TABLE service_provider ADD COLUMN business_contact VARCHAR(64);
ALTER TABLE service_provider ADD COLUMN business_phone VARCHAR(32);

ALTER TABLE person ADD COLUMN service_provider_id BIGINT;
ALTER TABLE person ADD COLUMN person_type VARCHAR(32);

CREATE TABLE service_provider_cooperation_scope_rel (
    id BIGINT PRIMARY KEY,
    service_provider_id BIGINT NOT NULL,
    scope_code VARCHAR(64) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_sp_scope_rel_spid ON service_provider_cooperation_scope_rel(service_provider_id);
CREATE INDEX idx_sp_scope_rel_scope ON service_provider_cooperation_scope_rel(scope_code);

INSERT INTO service_provider_cooperation_scope_rel (id, service_provider_id, scope_code, created_at, updated_at, deleted)
SELECT COALESCE((SELECT MAX(id) FROM service_provider_cooperation_scope_rel), 0) + ROW_NUMBER() OVER (ORDER BY src.id),
       src.id,
       src.scope_code,
       CURRENT_TIMESTAMP,
       CURRENT_TIMESTAMP,
       0
FROM (
    SELECT id,
           CASE type
               WHEN 'SUPPLIER' THEN 'HARDWARE_PROCUREMENT'
               WHEN 'SERVICE_PROVIDER' THEN 'SOFTWARE_DEVELOPMENT'
               WHEN 'INTEGRATOR' THEN 'INTEGRATION'
               WHEN 'MAINTENANCE' THEN 'OPERATIONS_SERVICE'
               ELSE NULL
           END AS scope_code
    FROM service_provider
) src
WHERE src.scope_code IS NOT NULL;

UPDATE service_provider
SET vendor_level = CASE rating_level
    WHEN 'S' THEN 'STRATEGIC_PARTNER'
    WHEN 'A' THEN 'CORE_SUPPLIER'
    WHEN 'B' THEN 'GENERAL_SUPPLIER'
    WHEN 'C' THEN 'GENERAL_SUPPLIER'
    ELSE vendor_level
END;

UPDATE service_provider
SET score = CASE rating_level
    WHEN 'S' THEN 5
    WHEN 'A' THEN 4
    WHEN 'B' THEN 3
    WHEN 'C' THEN 2
    ELSE score
END;

UPDATE person p
SET service_provider_id = (
    SELECT rel.service_provider_id
    FROM service_provider_person_rel rel
    WHERE rel.person_id = p.id
      AND rel.deleted = 0
    ORDER BY rel.created_at ASC, rel.id ASC
    LIMIT 1
)
WHERE p.service_provider_id IS NULL
  AND EXISTS (
    SELECT 1
    FROM service_provider_person_rel rel
    WHERE rel.person_id = p.id
      AND rel.deleted = 0
  );

UPDATE person
SET person_type = 'OPS'
WHERE EXISTS (
    SELECT 1
    FROM asset_hardware_person_rel rel
    WHERE rel.person_id = person.id
      AND rel.deleted = 0
      AND rel.relation_type = 'RESPONSIBLE'
);

UPDATE person
SET person_type = 'DEV'
WHERE person_type IS NULL;
