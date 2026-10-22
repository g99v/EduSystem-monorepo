--liquibase formatted sql

--changeset EduSystem:003-create-student-table
CREATE SEQUENCE t_student_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE t_student (
    id INTEGER NOT NULL,
    first_name VARCHAR(20),
    last_name VARCHAR(255),
    age INTEGER NOT NULL,
    school_id INTEGER,
    user_id INTEGER NOT NULL,
    CONSTRAINT pk_t_student PRIMARY KEY (id),
    CONSTRAINT uk_t_student_user UNIQUE (user_id),
    CONSTRAINT fk_t_student_school
        FOREIGN KEY (school_id) REFERENCES school (id),
    CONSTRAINT fk_t_student_user
        FOREIGN KEY (user_id) REFERENCES t_user (id)
);

--rollback DROP TABLE t_student;
--rollback DROP SEQUENCE t_student_seq;

