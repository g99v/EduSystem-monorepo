--liquibase formatted sql

--changeset EduSystem:015-revoke-legacy-local-kong-key dbms:postgresql
-- The original local credential was committed to Git. Never trust it again.
-- Each machine provisions a fresh verifier with scripts/local-secrets.py.
UPDATE t_internal_api_key
SET enabled = FALSE, revoked = TRUE
WHERE key_id = 'kong-local-01';

-- Revocation is deliberately not undone by a rollback.
--rollback SELECT 1;
