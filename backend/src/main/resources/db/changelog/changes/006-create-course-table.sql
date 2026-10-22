--liquibase formatted sql

--changeset EduSystem:006-create-course-table
CREATE SEQUENCE course_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE course (
    id INTEGER NOT NULL,
    name VARCHAR(100) NOT NULL,
    description VARCHAR(1000),
    teacher_name VARCHAR(100) NOT NULL,
    instructor_id INTEGER,
    CONSTRAINT pk_course PRIMARY KEY (id),
    CONSTRAINT fk_course_instructor
        FOREIGN KEY (instructor_id) REFERENCES t_instructor (id)
);

CREATE INDEX ix_course_instructor
    ON course (instructor_id);

--rollback DROP TABLE course;
--rollback DROP SEQUENCE course_seq;

