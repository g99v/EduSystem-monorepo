--liquibase formatted sql

--changeset EduSystem:001-create-user-table
CREATE SEQUENCE t_user_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE t_user (
    id INTEGER NOT NULL DEFAULT nextval('t_user_seq'),
    email VARCHAR(255) NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    role VARCHAR(20) NOT NULL,
    enabled BOOLEAN NOT NULL,
    CONSTRAINT pk_t_user PRIMARY KEY (id),
    CONSTRAINT uk_t_user_email UNIQUE (email),
    CONSTRAINT ck_t_user_role
        CHECK (role IN ('STUDENT', 'INSTRUCTOR', 'ADMIN'))
);

--rollback DROP TABLE t_user;
--rollback DROP SEQUENCE t_user_seq;

