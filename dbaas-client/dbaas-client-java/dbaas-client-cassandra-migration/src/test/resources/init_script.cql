CREATE KEYSPACE service_db WITH replication ={'class' : 'SimpleStrategy', 'replication_factor':1};
CREATE KEYSPACE tenant_db_a WITH replication ={'class' : 'SimpleStrategy', 'replication_factor':1};
CREATE KEYSPACE tenant_db_b WITH replication ={'class' : 'SimpleStrategy', 'replication_factor':1};

USE service_db;

CREATE TABLE testObjects
(
    id   varchar,
    name varchar,
    PRIMARY KEY (id)
);

INSERT INTO testObjects(id, name)
values ('object1', 'test object 1');

INSERT INTO testObjects(id, name)
values ('object2', 'test object 2');

INSERT INTO testObjects(id, name)
values ('multitenancy-object', 'not-defined');