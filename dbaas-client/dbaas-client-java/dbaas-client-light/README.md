# DBaaS Client Light

This client represents DbaaS REST API implementation on plane java (without Spring framework) with using OkHttpClient as a restclient. 

## Usage
Here are the steps describing how to use dbaas-client-light library: 
1. Add maven dependency: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-light</artifactId>
        <version>${dbaas-client-light.version}</version>
    </dependency>
    ```
    Where `${dbaas-client-light.version}` is the latest release version
    Omit tag `version` if you use [qubership-springboot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md). if you use [qubership-springboot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md). 
2. Implement all the necessary classes to make dbaas-client work with the database type you need. 
    For PostgreSQL database type there is an implementation with usage example in it's README.md: 
    [dbaas-client-postgres-base](../dbaas-client-postgres-base/README.md). You can use it as a sample. 
    
    Here are client implementation steps:
    1. Preconfigure `OkHttpClient` instance with the necessary authorization. 
    2. Use it to create `DbaasClient` instance: 
        ```java
        DbaasClient dbaasClient = new DbaaSClientOkHttpImpl(
            "http://dbaas-agent:8080", // address by which DBaaS API is available
            okHttpClient // OkHttpClient providing authorization if necessary
        );
        ```
    3. Use this `DbaasClient` to create `DatabasePool` instance: 
        ```java
        DatabasePool databasePool = new DatabasePool(dbaasClient,
            null,
            // past here your way of configuring microservice name or skip to
            // use environment  DatabasePool.MICROSERVICE_NAME_ENV instead
        
            null,
            // past here your way of configuring cloud namespace or skip to
            // use environment  DatabasePool.CLOUD_NAMESPACE_ENV instead
        
            Collections.singletonList(new PostConnectProcessor<PostgresDatabase>() {
                @Override
                public void process(PostgresDatabase database) {
                    // create DataSource using obtained connection properties
                    DataSource dataSource = createDataSource(database.getConnectionProperties());
                    // save reference on this DataSource in connectionProperties so it can be used by DbaasPostgresProxyDataSource
                    database.getConnectionProperties().setDataSource(dataSource);
                }
               
                @Override
                public Class<PostgresDatabase> getSupportedDatabaseType() {
                    return PostgresDatabase.class;
                }
            }), // list of PostConnectProcessors that can be used e.g. for DataSource configuration
                        
            null // ordering for PostConnectProcessors
        );
        ```
    4. Use `DatbasePool` instance to get or create database by type and classifier. 
        You can use `ServiceDbaaSClassifierBuilder` for service database creation, or create your own `DbaaSClassifierBuilder`. 
        ```java
        PostgresDatabase servicePostgresDb = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, new ServiceDbaaSClassifierBuilder(null).build());
        ```
