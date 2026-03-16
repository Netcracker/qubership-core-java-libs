CREATE SEQUENCE IF NOT EXISTS persons_id_seq AS INTEGER INCREMENT BY 1 MINVALUE 1;

CREATE TABLE IF NOT EXISTS persons (
    id INTEGER PRIMARY KEY DEFAULT nextval('persons_id_seq'),
    first_name VARCHAR,
    last_name VARCHAR
);