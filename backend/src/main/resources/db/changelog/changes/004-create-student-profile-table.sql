--liquibase formatted sql

--changeset EduSystem:004-create-student-profile-table
CREATE SEQUENCE student_profile_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE student_profile (
    id INTEGER NOT NULL,
    bio VARCHAR(255),
    student_id INTEGER,
    CONSTRAINT pk_student_profile PRIMARY KEY (id),
    CONSTRAINT uk_student_profile_student UNIQUE (student_id),
    CONSTRAINT fk_student_profile_student
        FOREIGN KEY (student_id) REFERENCES t_student (id)
);

--rollback DROP TABLE student_profile;
--rollback DROP SEQUENCE student_profile_seq;

