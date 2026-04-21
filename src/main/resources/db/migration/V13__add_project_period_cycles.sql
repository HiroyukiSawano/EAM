CREATE TABLE project_period (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    stage_name VARCHAR(64) NOT NULL,
    planned_date DATE NOT NULL,
    actual_date DATE NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_period_project_id ON project_period(project_id);

CREATE TABLE project_payment_cycle (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    stage_name VARCHAR(64) NOT NULL,
    payment_ratio DECIMAL(7,2) NOT NULL,
    payment_amount DECIMAL(12,2) NOT NULL,
    planned_payment_date DATE NOT NULL,
    actual_payment_date DATE NULL,
    sort_order INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_project_payment_cycle_project_id ON project_payment_cycle(project_id);

INSERT INTO project_period (project_id, stage_name, planned_date, actual_date, sort_order, created_at, updated_at, deleted)
SELECT id, '立项', approval_date, NULL, 1, NOW(), NOW(), 0
FROM project_info
WHERE approval_date IS NOT NULL;

INSERT INTO project_period (project_id, stage_name, planned_date, actual_date, sort_order, created_at, updated_at, deleted)
SELECT id, '开工', start_date, NULL, 2, NOW(), NOW(), 0
FROM project_info
WHERE start_date IS NOT NULL;

INSERT INTO project_period (project_id, stage_name, planned_date, actual_date, sort_order, created_at, updated_at, deleted)
SELECT id, '初验', initial_delivery_date, NULL, 3, NOW(), NOW(), 0
FROM project_info
WHERE initial_delivery_date IS NOT NULL;

INSERT INTO project_period (project_id, stage_name, planned_date, actual_date, sort_order, created_at, updated_at, deleted)
SELECT id, '终验', end_date, NULL, 4, NOW(), NOW(), 0
FROM project_info
WHERE end_date IS NOT NULL;

INSERT INTO project_period (project_id, stage_name, planned_date, actual_date, sort_order, created_at, updated_at, deleted)
SELECT id, '质保截止', warranty_end_date, NULL, 5, NOW(), NOW(), 0
FROM project_info
WHERE warranty_end_date IS NOT NULL;

INSERT INTO project_payment_cycle (
    project_id,
    stage_name,
    payment_ratio,
    payment_amount,
    planned_payment_date,
    actual_payment_date,
    sort_order,
    created_at,
    updated_at,
    deleted
)
SELECT
    id,
    COALESCE(NULLIF(payment_cycle_name, ''), '资金支付'),
    payment_ratio,
    payment_amount,
    planned_payment_date,
    actual_payment_date,
    1,
    NOW(),
    NOW(),
    0
FROM project_info
WHERE payment_ratio IS NOT NULL
  AND payment_amount IS NOT NULL
  AND planned_payment_date IS NOT NULL;

UPDATE project_info
SET payment_status = CASE
    WHEN actual_payment_date IS NULL THEN 'PENDING'
    ELSE 'PAID'
END
WHERE (payment_status IS NULL OR payment_status = '')
  AND payment_ratio IS NOT NULL
  AND payment_amount IS NOT NULL
  AND planned_payment_date IS NOT NULL;
