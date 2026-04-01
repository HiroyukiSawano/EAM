ALTER TABLE person
    ADD COLUMN has_ops_account TINYINT(1) NOT NULL DEFAULT 0;

UPDATE person
SET has_ops_account = 0
WHERE has_ops_account IS NULL;
