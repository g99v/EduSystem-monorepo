--liquibase formatted sql

--changeset EduSystem:005-create-instructor-table
CREATE SEQUENCE t_instructor_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE t_instructor (
    id INTEGER NOT NULL,
    first_name VARCHAR(20),
    last_name VARCHAR(255),
    user_id INTEGER NOT NULL,
    CONSTRAINT pk_t_instructor PRIMARY KEY (id),
    CONSTRAINT uk_t_instructor_user UNIQUE (user_id),
    CONSTRAINT fk_t_instructor_user
        FOREIGN KEY (user_id) REFERENCES t_user (id)
);

--rollback DROP TABLE t_instructor;
--rollback DROP SEQUENCE t_instructor_seq;

