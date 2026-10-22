--liquibase formatted sql

--changeset EduSystem:014-provision-local-kong-api-key dbms:postgresql
-- Only the verifier is stored here. Kong reads the raw credential from its secret file.
INSERT INTO t_internal_api_key (key_id, client_id, key_hash, enabled, revoked)
VALUES ('kong-local-01', 'kong', decode('0f074a905566209f01805e38894d29cb3357073e4c20385a2d668dffaebf91cc', 'hex'), TRUE, FALSE);

INSERT INTO t_internal_api_key_authority (key_id, authority)
VALUES ('kong-local-01', 'INTERNAL_AUTH_VERIFY');

--rollback DELETE FROM t_internal_api_key WHERE key_id = 'kong-local-01';
