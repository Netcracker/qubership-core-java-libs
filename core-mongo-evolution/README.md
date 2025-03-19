![Coverage](https://sonarcloud.io/api/project_badges/measure?metric=coverage&project=Netcracker_qubership-core-mongo-evolution)
![duplicated_lines_density](https://sonarcloud.io/api/project_badges/measure?metric=duplicated_lines_density&project=Netcracker_qubership-core-mongo-evolution)
![vulnerabilities](https://sonarcloud.io/api/project_badges/measure?metric=vulnerabilities&project=Netcracker_qubership-core-mongo-evolution)
![bugs](https://sonarcloud.io/api/project_badges/measure?metric=bugs&project=Netcracker_qubership-core-mongo-evolution)
![code_smells](https://sonarcloud.io/api/project_badges/measure?metric=code_smells&project=Netcracker_qubership-core-mongo-evolution)

# Mongo-evolution library

## Overview

Library updates microservice or tenant Mongo database.
DB migration tool is necessary for an automated version control, ensuring that all data are synchronized.

When requests DB connection, the library
1. scans classes annotated with @ChangeLog,
2. checks current DB version,
3. updates DB if there is @ChangeLog with higher version. 

### Modules

This repository contains mongo evolution modules which based on pure java with mongo client and Spring framework:

* [Java mongo evolution](./mongo-evolution-java/README.md) 
* [Spring mongo evolution](./mongo-evolution-spring/README.md)

Spring mongo evolution allows to use MongoTemplate in evolution code.

### Migration from 3.x to 4.x

Main changes:
* Move to `mongodb-driver-sync` 4.x  
* Remove `jongo` dependency 

Mongo-evolution 3.x was based on `MongoDB Java Driver 3.x` and it was incompatible with 
`spring-mongodb-data 2.4.x`, so it made us move to `mongodb-driver 4.x`. Additionally, project `jongo` does not support
4.x mongodb driver, that's why we delete is from support. In order to move to `mongo-evolution 4.x`, you should do the following:
1) Build and pass to mongo-evolution a new `com.mongodb.client.MongoClient` instead of `com.mongodb.MongoClient`  
2) Get rid of `jongo` from your migration scripts and use `MongoDatabase` instead. For example:

mongo-evolution 3.x:   
```java
    @ChangeSet(order = 4)
    public void jongoUpdate(Jongo jongo) {
        log.debug("@ChangeSet Jongo.class");
        org.jongo.MongoCollection collection = jongo.getCollection(TestConstants.COLLECTION_NAME);

        TestObject newObj = new TestObject();
        newObj.setName(TestConstants.COLUMN_VALUE_UPDATE);
        Assert.assertNotNull(collection.update(TestConstants.JSON_STRING).with(newObj));

        MongoCursor<TestObject> result = collection.find(TestConstants.JSON_STRING_UPDATE).as(TestObject.class);
        Assert.assertEquals(1, result.count());

        Iterator<TestObject> iter = result.iterator();
        Assert.assertEquals(TestConstants.COLUMN_VALUE_UPDATE, iter.next().getName());

    }
```

mongo-evolution 4.x:   
```java
    @ChangeSet(order = 4)
    public void updateDocumentUsingMongoDatabase(MongoDatabase mongoDatabase) {
        log.debug("@ChangeSet update document using mongoDatabase");
        MongoCollection<TestObject> collection = mongoDatabase.getCollection(COLLECTION_NAME, TestObject.class);
        Bson filterObject = Filters.eq(COLUMN_NAME, COLUMN_VALUE);
        Bson updated = Updates.set("name", COLUMN_VALUE_UPDATE);
        Assert.assertNotNull(collection.updateOne(filterObject, updated));

        Assert.assertEquals(1, collection.countDocuments());

        MongoCursor<TestObject> iterator = collection.find(Filters.eq(COLUMN_NAME, COLUMN_VALUE_UPDATE)).iterator();

        Assert.assertEquals(COLUMN_VALUE_UPDATE, iterator.next().getName());

    }
```
**Note:** If you pass `mongoDatabase` object through `DBManagerEntity` and use `POJO` instead of `Documents` 
then you should bother to add `PojoCodec` to mongoDatabase.

mongo-evolution-spring 4.x: 

```java
    @ChangeSet(order = 4)
    public void updateDocumentUsingMongoTemplate(MongoTemplate mongoTemplate) {
        log.debug("@ChangeSet update document using mongoTemplate");
        String columnValueUpdate = "update";
        mongoTemplate.updateFirst(Query.query(
                Criteria.where(COLUMN_NAME).is(COLUMN_VALUE)),
                Update.update(COLUMN_NAME, columnValueUpdate), TestConstants.COLLECTION_NAME);

        List<TestObject> testObjects = mongoTemplate.find(Query.query(Criteria.where(COLUMN_NAME).is(columnValueUpdate)), TestObject.class, TestConstants.COLLECTION_NAME);
        Assert.assertEquals(1, testObjects.size());
        Assert.assertEquals(columnValueUpdate, testObjects.get(0).getName());

    }
```
