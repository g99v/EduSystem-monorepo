--liquibase formatted sql

--changeset EduSystem:011-create-announcement-table
CREATE SEQUENCE announcement_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE announcement (
    id INTEGER NOT NULL,
    course_id INTEGER NOT NULL,
    title VARCHAR(200) NOT NULL,
    body TEXT NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL,
    CONSTRAINT pk_announcement PRIMARY KEY (id),
    CONSTRAINT fk_announcement_course
        FOREIGN KEY (course_id) REFERENCES course (id) ON DELETE CASCADE
);

CREATE INDEX ix_announcement_course_created_at
    ON announcement (course_id, created_at, id);

--rollback DROP TABLE announcement;
--rollback DROP SEQUENCE announcement_seq;
