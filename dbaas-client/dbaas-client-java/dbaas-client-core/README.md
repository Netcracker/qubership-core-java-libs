# DBaaS Client Core

Basic and not database specific DBaaS REST API client implementation based on Spring Framework. 
You may use it if you want to just get a connection properties without creating a database client.

## Usage

For the most cases it's recommended to use database specific starters instead of this library:
* [dbaas-client-postgres-starter](../dbaas-client-postgres-starter/README.md)
* [dbaas-client-mongo-starter](../dbaas-client-mongo-starter/README.md)
* [dbaas-client-cassandra-starter](../dbaas-client-cassandra-starter/README.md)
* [dbaas-client-opensearch-starter](../dbaas-client-opensearch-starter/README.md)
* [dbaas-client-cassandra-starter](../dbaas-client-cassandra-starter/README.md)

If you still decided to use dbaas-client-core, follow these steps: 
1. Add maven dependency: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-core</artifactId>
        <version>${dbaas-client-core.version}</version>
    </dependency>
   
   and one of the following dependencies
   -------------------------
   if you use resstemplate:
   
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-resttemplate</artifactId>
        <version>{version}</version>
    </dependency>
   
   -------------------------
   if you use webclient:
   
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-webclient</artifactId>
        <version>{version}</version>
    </dependency>
    ```
    Where `${dbaas-client-core.version}` is the latest release version
    Omit tag `version` if you use [qubership-springboot-starter-parent](<github link todo>/springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md). 
2. Add `@EnableDbaasDefault` annotation to one of your application configuration classes. 
3. Implement interfaces and abstract classes `DatabaseType`, `AbstractDatabase` and `DatabaseConnection` so they 
    represent the database type you need.  
    
    Find example in dbaas-client-postgres-base implementations of postgreSQL database type: 
    [PostgresDBType](<github link todo>/dbaas-client/blob/master/dbaas-client-java/dbaas-client-postgres-base/src/main/java/com/netcracker/cloud/dbaas/client/entity/database/type/PostgresDBType.java).  
4. Autowire `DatabasePool` and `DbaasClassifierFactory` beans to obtain databases from DBaaS: 
    ```java
    @Autowired
    private DbaasClassifierFactory dbaasClassifierFactory;
    
    @Autowired
    private DatabasePool databasePool;
    
    public void createDatabases() {
        DbaasDbClassifier tenantDbClassifier = dbaasClassifierFactory.newTenantClassifierBuilder().build();
        DbaasDbClassifier serviceDbClassifier = dbaasClassifierFactory.newServiceClassifierBuilder().build();
    
        MyDatabase tenantDb = databasePool.getOrCreateDatabase(MyDBType.INSTANCE, tenantDbClassifier);
        MyDatabase serviceDb = databasePool.getOrCreateDatabase(MyDBType.INSTANCE, serviceDbClassifier);
    
        // do something with tenantDb and serviceDb objects which represent databases of your type (see step 4)
    
    }
    ``` 
## Logical database provider feature
There may be situations when you don't need to create a `logical database` through DbaaS
aggregator but you want to use `dbaas-client`. It can be useful during local development or
when using different profiles. This can be achieved by providing information about a logical database by yourself.   
So, in order to use this features you should do the following things:

1) Implement `LogicalDbProvider` or use a ready-made implementation (PostgresqlLogicalDbProvider, OpensearchLogicalDbProvider, CassandraLogicalDbProvider, etc);
2) create baen of your implementation.  
   *For example:*
```java
@Bean
public PostgresqlLogicalDbProviderImpl postgresqlLogicalDbProviderImpl(){
    return new PostgresqlLogicalDbProviderImpl();
}

    
public class PostgresqlLogicalDbProviderImpl extends PostgresqlLogicalDbProvider {
    @Override
    public int order() {
        return 1;
    }

    @Override
    // method may return NULL
    public PostgresConnectionProperty provideConnectionProperty(SortedMap<String, Object> classifier, DatabaseConfig params) {
        return new PostgresConnectionProperty("url", "username", "password", "type of grants, e.g. admin");
    }
}
```  
That's all but there are some nuances:
* Provider with the lowest order will be processed first.
  Default LogicalDbProvider has `0` value.
*  Method `LogicalDbProvider#provideConnectionProperty` or `LogicalDbProvider#provide` may return NULL value. In this case we iterate to next provider
   in an order. If you need to interrupt processing you have to throw an exception.
* You can use `LogicalDbProvider#provideDatabaseInfo` to provide additional data, but in many cases it's not necessary.
