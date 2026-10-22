--liquibase formatted sql

--changeset EduSystem:007-create-enrollment-table
CREATE SEQUENCE enrollment_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE enrollment (
    id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    course_id INTEGER NOT NULL,
    enrolled_at TIMESTAMP WITH TIME ZONE NOT NULL,
    status VARCHAR(20) NOT NULL,
    grade VARCHAR(10),
    CONSTRAINT pk_enrollment PRIMARY KEY (id),
    CONSTRAINT uk_enrollment_student_course UNIQUE (student_id, course_id),
    CONSTRAINT fk_enrollment_student
        FOREIGN KEY (student_id) REFERENCES t_student (id),
    CONSTRAINT fk_enrollment_course
        FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT ck_enrollment_status
        CHECK (status IN ('ACTIVE', 'COMPLETED', 'WITHDRAWN'))
);

CREATE INDEX ix_enrollment_course_id
    ON enrollment (course_id);

--rollback DROP TABLE enrollment;
--rollback DROP SEQUENCE enrollment_seq;

