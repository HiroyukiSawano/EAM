UPDATE sys_department SET status = 'ACTIVE' WHERE status = '正常';
UPDATE sys_department SET status = 'INACTIVE' WHERE status = '停用';

UPDATE person SET status = 'ACTIVE' WHERE status = '正常';
UPDATE person SET status = 'INACTIVE' WHERE status = '停用';

UPDATE service_provider SET status = 'ACTIVE' WHERE status = '正常';
UPDATE service_provider SET status = 'INACTIVE' WHERE status = '停用';

UPDATE information_system SET status = 'ACTIVE' WHERE status = '正常';
UPDATE information_system SET status = 'INACTIVE' WHERE status = '停用';
