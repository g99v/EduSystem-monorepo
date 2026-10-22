--liquibase formatted sql

--changeset EduSystem:013-create-internal-api-key-tables
CREATE TABLE t_internal_api_key (
    key_id VARCHAR(64) NOT NULL,
    client_id VARCHAR(64) NOT NULL,
    key_hash BYTEA NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    revoked BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_t_internal_api_key PRIMARY KEY (key_id),
    CONSTRAINT ck_t_internal_api_key_hash CHECK (octet_length(key_hash) = 32),
    CONSTRAINT ck_t_internal_api_key_client CHECK (length(trim(client_id)) > 0),
    CONSTRAINT ck_t_internal_api_key_expiry CHECK (expires_at IS NULL OR expires_at > created_at)
);

CREATE INDEX ix_t_internal_api_key_client ON t_internal_api_key (client_id);

CREATE TABLE t_internal_api_key_authority (
    key_id VARCHAR(64) NOT NULL,
    authority VARCHAR(100) NOT NULL,
    CONSTRAINT pk_t_internal_api_key_authority PRIMARY KEY (key_id, authority),
    CONSTRAINT fk_t_internal_api_key_authority_key
        FOREIGN KEY (key_id) REFERENCES t_internal_api_key (key_id) ON DELETE CASCADE,
    CONSTRAINT ck_t_internal_api_key_authority CHECK (length(trim(authority)) > 0)
);

--rollback DROP TABLE t_internal_api_key_authority;
--rollback DROP TABLE t_internal_api_key;
