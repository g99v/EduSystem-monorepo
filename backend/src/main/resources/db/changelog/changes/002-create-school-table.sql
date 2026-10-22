--liquibase formatted sql

--changeset EduSystem:002-create-school-table
CREATE SEQUENCE school_seq
    START WITH 1
    INCREMENT BY 1;

CREATE TABLE school (
    id INTEGER NOT NULL,
    name VARCHAR(255),
    CONSTRAINT pk_school PRIMARY KEY (id)
);

--rollback DROP TABLE school;
--rollback DROP SEQUENCE school_seq;
