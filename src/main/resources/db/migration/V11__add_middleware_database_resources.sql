CREATE TABLE middleware_resource (
    id BIGINT PRIMARY KEY,
    middleware_code VARCHAR(64) NOT NULL,
    middleware_name VARCHAR(128) NOT NULL,
    middleware_type VARCHAR(64) NOT NULL,
    version VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_middleware_resource_code_deleted
    ON middleware_resource(middleware_code, deleted);
CREATE INDEX idx_middleware_resource_type ON middleware_resource(middleware_type);
CREATE INDEX idx_middleware_resource_status ON middleware_resource(status);

CREATE TABLE database_resource (
    id BIGINT PRIMARY KEY,
    database_code VARCHAR(64) NOT NULL,
    database_name VARCHAR(128) NOT NULL,
    database_type VARCHAR(64) NOT NULL,
    version VARCHAR(64),
    status VARCHAR(32) NOT NULL DEFAULT 'ACTIVE',
    remark VARCHAR(255),
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_database_resource_code_deleted
    ON database_resource(database_code, deleted);
CREATE INDEX idx_database_resource_type ON database_resource(database_type);
CREATE INDEX idx_database_resource_status ON database_resource(status);

CREATE TABLE software_middleware_rel (
    id BIGINT PRIMARY KEY,
    software_id BIGINT NOT NULL,
    middleware_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_software_middleware_rel
    ON software_middleware_rel(software_id, middleware_id, deleted);
CREATE INDEX idx_software_middleware_rel_software ON software_middleware_rel(software_id);
CREATE INDEX idx_software_middleware_rel_middleware ON software_middleware_rel(middleware_id);

CREATE TABLE software_database_rel (
    id BIGINT PRIMARY KEY,
    software_id BIGINT NOT NULL,
    database_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_software_database_rel
    ON software_database_rel(software_id, database_id, deleted);
CREATE INDEX idx_software_database_rel_software ON software_database_rel(software_id);
CREATE INDEX idx_software_database_rel_database ON software_database_rel(database_id);
