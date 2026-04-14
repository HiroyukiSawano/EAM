ALTER TABLE information_system
    ADD COLUMN owner_name VARCHAR(64) NULL;

UPDATE information_system
SET owner_name = COALESCE(owner_name, (
    SELECT p.name
    FROM person p
    WHERE p.id = information_system.owner_person_id
    LIMIT 1
));

UPDATE information_system
SET owner_person_id = NULL
WHERE owner_person_id IS NOT NULL;
