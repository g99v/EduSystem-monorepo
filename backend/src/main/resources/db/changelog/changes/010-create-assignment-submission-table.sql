--liquibase formatted sql

--changeset EduSystem:010-create-assignment-submission-table
CREATE SEQUENCE assignment_submission_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE assignment_submission (
    id INTEGER NOT NULL,
    assignment_id INTEGER NOT NULL,
    student_id INTEGER NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    submitted_at TIMESTAMP WITH TIME ZONE NOT NULL,
    grade INTEGER,
    feedback TEXT,
    graded_at TIMESTAMP WITH TIME ZONE,
    CONSTRAINT pk_assignment_submission PRIMARY KEY (id),
    CONSTRAINT uk_assignment_submission_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_assignment_submission_assignment
        FOREIGN KEY (assignment_id) REFERENCES assignment (id),
    CONSTRAINT fk_assignment_submission_student
        FOREIGN KEY (student_id) REFERENCES t_student (id),
    CONSTRAINT ck_assignment_submission_file_size
        CHECK (file_size >= 0),
    CONSTRAINT ck_assignment_submission_grade
        CHECK (grade IS NULL OR grade BETWEEN 0 AND 25)
);

CREATE INDEX ix_assignment_submission_assignment
    ON assignment_submission (assignment_id);

CREATE INDEX ix_assignment_submission_student
    ON assignment_submission (student_id);

--rollback DROP TABLE assignment_submission;
--rollback DROP SEQUENCE assignment_submission_seq;

