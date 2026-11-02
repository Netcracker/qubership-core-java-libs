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
    1. Create `DbaasClient` instance. It authorizes its requests to DBaaS on its own, so no `OkHttpClient` and no token
       handling is needed here:
        ```java
        DbaasClient dbaasClient = DbaasClientFactory.newDbaasClient();
        ```
       With `M2M_AUTH_MODE` set to `hybrid` or `k8s` requests carry a kubernetes service account token for DBaaS and go
       to `api.dbaas.address` (`API_DBAAS_ADDRESS`), which `k8s` requires and `hybrid` falls back from to the agent
       address. Otherwise, they carry a legacy token and go to `dbaas.api.address`
       (`DBAAS_API_ADDRESS`, `http://dbaas-agent:8080` by default).
       `DbaasClientFactory.builder()` overrides the addresses and the mode, and puts own interceptors on top of the
       ones the library provides:
        ```java
        DbaasClient dbaasClient = DbaasClientFactory.builder()
            .httpClient(builder -> builder.addInterceptor(new MyInterceptor()))
            .build();
        ```
       Creating the client as `new DbaaSClientOkHttpImpl(address, okHttpClient)` stays available for the cases the
       factory does not cover. Such an `OkHttpClient` must provide authorization itself.
    2. Use this `DbaasClient` to create `DatabasePool` instance: 
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
    3. Use `DatbasePool` instance to get or create database by type and classifier. 
        You can use `ServiceDbaaSClassifierBuilder` for service database creation, or create your own `DbaaSClassifierBuilder`. 
        ```java
        PostgresDatabase servicePostgresDb = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, new ServiceDbaaSClassifierBuilder(null).build());
        ```
