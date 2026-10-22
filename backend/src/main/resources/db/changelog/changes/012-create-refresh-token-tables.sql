--liquibase formatted sql

--changeset EduSystem:012-create-refresh-token-tables
CREATE TABLE refresh_token_family (
    id UUID NOT NULL,
    user_id INTEGER NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    expires_at TIMESTAMP WITH TIME ZONE NOT NULL,
    revoked_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_refresh_token_family PRIMARY KEY (id),
    CONSTRAINT fk_refresh_token_family_user
        FOREIGN KEY (user_id) REFERENCES t_user (id) ON DELETE CASCADE,
    CONSTRAINT ck_refresh_token_family_expiry CHECK (expires_at > created_at)
);

CREATE TABLE refresh_token (
    id UUID NOT NULL,
    family_id UUID NOT NULL,
    token_hash VARCHAR(64) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    consumed_at TIMESTAMP WITH TIME ZONE,
    replaced_by UUID,
    CONSTRAINT pk_refresh_token PRIMARY KEY (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash),
    CONSTRAINT fk_refresh_token_family
        FOREIGN KEY (family_id) REFERENCES refresh_token_family (id) ON DELETE CASCADE,
    CONSTRAINT fk_refresh_token_replacement
        FOREIGN KEY (replaced_by) REFERENCES refresh_token (id) ON DELETE SET NULL
);

CREATE INDEX ix_refresh_token_family_user ON refresh_token_family (user_id);
CREATE INDEX ix_refresh_token_family_expiry ON refresh_token_family (expires_at);
CREATE INDEX ix_refresh_token_family ON refresh_token (family_id);

--rollback DROP TABLE refresh_token;
--rollback DROP TABLE refresh_token_family;
