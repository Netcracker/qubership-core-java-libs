# Java mongo-evolution library

## Overview

Allows performing mongo evolution and use. In order to use mongo evolution library you should:

### Add maven dependency

Add the dependency to pom:

```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>mongo-evolution</artifactId>
        <version>${mongo.evolution.version}</version>
    </dependency>
```

### Write mongo evolution code

Create plain java classes with mongo evolutions code. Each class must contains @ChangeLog annotation with higher version 
and methods which annotated @ChangeSet and have parameter as MongoDatabase or without argument parameter.

For example:

```java
package com.netcracker.cloud.mongoevolution;

@ChangeLog(version = 1)
public class ChangeLog1 {
   
    @ChangeSet
    public void deletePassForActiveTenant(MongoDatabase mongoDatabase) {
        ...
    }

    @ChangeSet
    public void deletePassForWaitedTenant() {
        ...
    }
}
```

### Configure mongo evolution

In the last step you should specify package where you mongo evolution code are and run evolution. For example:

```java
public void configureAndRunMongoEvo() {
    new MongoEvolution(client, "my-db-name", new ConnectionSearchKey(null, DB_CLASSIFIER))
                        .evolve("com.netcracker.cloud.mongoevolution");
}
```
