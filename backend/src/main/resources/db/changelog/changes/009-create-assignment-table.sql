--liquibase formatted sql

--changeset EduSystem:009-create-assignment-table
CREATE SEQUENCE assignment_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE assignment (
    id INTEGER NOT NULL,
    course_id INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    assignment_instructions TEXT,
    max_grade INTEGER NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_assignment PRIMARY KEY (id),
    CONSTRAINT uk_assignment_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_assignment_course
        FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT ck_assignment_max_grade
        CHECK (max_grade BETWEEN 0 AND 25),
    CONSTRAINT ck_assignment_file_size
        CHECK (file_size >= 0),
    CONSTRAINT ck_assignment_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX ix_assignment_course_display_order
    ON assignment (course_id, display_order, id);

--rollback DROP TABLE assignment;
--rollback DROP SEQUENCE assignment_seq;

