CREATE TABLE service_provider_person_rel (
    id BIGINT PRIMARY KEY,
    service_provider_id BIGINT NOT NULL,
    person_id BIGINT NOT NULL,
    created_at TIMESTAMP,
    updated_at TIMESTAMP,
    deleted TINYINT NOT NULL DEFAULT 0
);

CREATE INDEX idx_service_provider_person_rel_spid ON service_provider_person_rel(service_provider_id);
CREATE INDEX idx_service_provider_person_rel_pid ON service_provider_person_rel(person_id);
