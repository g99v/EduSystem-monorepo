--liquibase formatted sql

--changeset EduSystem:008-create-lecture-table
CREATE SEQUENCE lecture_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE lecture (
    id INTEGER NOT NULL,
    course_id INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    original_filename VARCHAR(255) NOT NULL,
    storage_key VARCHAR(500) NOT NULL,
    content_type VARCHAR(150) NOT NULL,
    file_size BIGINT NOT NULL,
    display_order INTEGER NOT NULL,
    uploaded_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_lecture PRIMARY KEY (id),
    CONSTRAINT uk_lecture_storage_key UNIQUE (storage_key),
    CONSTRAINT fk_lecture_course
        FOREIGN KEY (course_id) REFERENCES course (id),
    CONSTRAINT ck_lecture_file_size
        CHECK (file_size >= 0),
    CONSTRAINT ck_lecture_display_order
        CHECK (display_order >= 0)
);

CREATE INDEX ix_lecture_course_display_order
    ON lecture (course_id, display_order, id);

--rollback DROP TABLE lecture;
--rollback DROP SEQUENCE lecture_seq;

