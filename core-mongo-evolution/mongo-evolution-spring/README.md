# Spring mongo-evolution library

## Overview

This library is wrapper under [java mongo evolution](../mongo-evolution-java/README.md) and allows using MongoTemplate in your mongo evolution scripts which 
is very conveniently if you use Spring framework in your microservice.  

First of all you should add the following maven dependency to your pom:

```xml
    <dependency>
        <groupId>org.qubership.cloud</groupId>
        <artifactId>mongo-evolution-spring</artifactId>
        <version>${mongo.evolution.spring.version}</version>
    </dependency>
``` 

Then add mongo evolution code as is described [here](../mongo-evolution-java/README.md) but to all this you can add 
methods with MongoTemplate arguments, for example: 

```java
    @ChangeSet
    public void deletePassForActiveTenant(MongoTemplate mongoTemplate) {
        ...
    }
```

Finally, specify where your mongo evolution code are and run evolution:

```java
            new SpringMongoEvolution(client, "my-db-name",
                    new ConnectionSearchKey(null, DB_CLASSIFIER))
                    .evolve(MongoDbConstants.CHANGELOGS_SCAN_PACKAGE);
```

Please pay attention that instead of `MongoEvolution` you should use `SpringMongoEvolution` class.  