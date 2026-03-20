CREATE TABLE sys_department (
    id BIGINT PRIMARY KEY,
    parent_id BIGINT,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_role (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user (
    id BIGINT PRIMARY KEY,
    username VARCHAR(64) NOT NULL,
    display_name VARCHAR(128) NOT NULL,
    department_id BIGINT,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE sys_user_role (
    id BIGINT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    role_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_location (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    site VARCHAR(128),
    building VARCHAR(128),
    floor VARCHAR(64),
    area VARCHAR(128),
    address_detail VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE service_provider (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    unified_social_credit_code VARCHAR(64),
    type VARCHAR(64) NOT NULL,
    rating_level VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE person (
    id BIGINT PRIMARY KEY,
    name VARCHAR(64) NOT NULL,
    gender VARCHAR(16),
    id_card_no VARCHAR(32),
    mobile VARCHAR(32),
    employee_no VARCHAR(64),
    photo_url VARCHAR(255),
    account VARCHAR(64),
    department_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE information_system (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    system_type VARCHAR(64) NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE project_info (
    id BIGINT PRIMARY KEY,
    code VARCHAR(64) NOT NULL,
    name VARCHAR(128) NOT NULL,
    project_type VARCHAR(64) NOT NULL,
    project_status VARCHAR(64) NOT NULL,
    start_date DATE,
    end_date DATE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware (
    id BIGINT PRIMARY KEY,
    asset_code VARCHAR(64) NOT NULL,
    asset_name VARCHAR(128) NOT NULL,
    hardware_category VARCHAR(64) NOT NULL,
    location_id BIGINT,
    department_id BIGINT,
    management_ip VARCHAR(64),
    business_ip VARCHAR(64),
    cpu_model VARCHAR(128),
    cpu_cores INT,
    memory_gb INT,
    hardware_status VARCHAR(64) NOT NULL,
    enabled_date DATE,
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_server (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    operating_system VARCHAR(128),
    disk_gb INT,
    virtualization VARCHAR(128),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_query_terminal (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    screen_size VARCHAR(64),
    touch_enabled TINYINT,
    device_model VARCHAR(128),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_ticket_terminal (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    printer_model VARCHAR(128),
    support_qr TINYINT,
    device_model VARCHAR(128),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_self_service_terminal (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    terminal_type VARCHAR(128),
    screen_size VARCHAR(64),
    device_model VARCHAR(128),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_system_rel (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    information_system_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_person_rel (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_hardware_vendor_rel (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    service_provider_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE system_person_rel (
    id BIGINT PRIMARY KEY,
    information_system_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE system_vendor_rel (
    id BIGINT PRIMARY KEY,
    information_system_id BIGINT NOT NULL,
    service_provider_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE project_system_rel (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    information_system_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE project_person_rel (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    relation_type VARCHAR(64) NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE project_vendor_rel (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    service_provider_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE project_hardware_rel (
    id BIGINT PRIMARY KEY,
    project_id BIGINT NOT NULL,
    hardware_asset_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE asset_lifecycle_record (
    id BIGINT PRIMARY KEY,
    hardware_asset_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    from_status VARCHAR(64),
    to_status VARCHAR(64) NOT NULL,
    reason VARCHAR(255),
    operator VARCHAR(64),
    action_time TIMESTAMP NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE TABLE audit_log (
    id BIGINT PRIMARY KEY,
    resource_type VARCHAR(64) NOT NULL,
    resource_id BIGINT NOT NULL,
    action_type VARCHAR(64) NOT NULL,
    content VARCHAR(1000),
    operator VARCHAR(64),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_sys_department_code ON sys_department(code);
CREATE UNIQUE INDEX uk_asset_location_code ON asset_location(code);
CREATE UNIQUE INDEX uk_service_provider_code ON service_provider(code);
CREATE UNIQUE INDEX uk_information_system_code ON information_system(code);
CREATE UNIQUE INDEX uk_project_code ON project_info(code);
CREATE UNIQUE INDEX uk_asset_hardware_code ON asset_hardware(asset_code);

INSERT INTO sys_department (id, parent_id, code, name, status, created_at, updated_at, deleted)
VALUES (1, NULL, 'ROOT', 'Root Department', 'ACTIVE', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP, 0);
