# DBaaS Client Postgres Base

Plane java postgresql datasource implemantation. This library does not contain spring dependencies and 
if you do not use spring too then you can take this library for using postgresql datasource created by dbaas client.

## Usage
Here are the steps describing how to use dbaas-client-postgres-base library: 
1. Add maven dependency: 
    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-postgres-base</artifactId>
        <version>${dbaas-client-postgres-base.version}</version>
    </dependency>
    ```
    Where `${dbaas-client-postgres-base.version}` is the latest release version
    Omit tag `version` if you use [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md). if you use [qubership-springboot-starter-parent](hhttps://github.com/Netcracker/qubership-core-springboot-starter) or [dbaas-client-bom](../../dbaas-client-bom-parent/dbaas-client-bom/README.md).
2. Add [PostgreSQL JDBC driver](https://jdbc.postgresql.org/) to your application dependencies
3. Initialize `DbaasClient` and `DatabasePool` to work with DBaaS REST API. The easiest way is to use [dbaas-client-light](../dbaas-client-light/README.md)
    module which provides `DbaasClient` implementation based on OkHttp3 library. 
    The following code snippet shows how to use `dbaas-client-postgres-base` with `dbaas-client-light`:  
    ```java
    public void doTheThing() {
        DbaasClient dbaasClient = new DbaaSClientOkHttpImpl(
                "http://dbaas-agent:8080", // address by which DBaaS API is available
                okHttpClient // OkHttpClient providing authorization if necessary
        );

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

        DbaaSChainClassifierBuilder myClassifierBuilder = new DbaaSChainClassifierBuilder(null) {
            // override methods to add your custom database classifier fields
        };

        // create proxy data source that will use classifier builder to resolve requested DB:
        // by default service database will be created
        DataSource proxyDataSourceForServiceDb = new DbaasPostgresProxyDataSource(databasePool);
        // or use your custom classifier builder, e.g. for building tenant-aware classifiers
        DataSource proxyDataSourceForDbWithMyClassifierBuilder = new DbaasPostgresProxyDataSource(databasePool, myClassifierBuilder);

        // everything is ready by now, use them as regular DataSources

        // alternatively instead of using DbaasPostgresProxyDataSource you can use DatabasePool directly to obtain database
        // use ServiceDbaaSClassifierBuilder for service database creation
        PostgresDatabase servicePostgresDb = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, new ServiceDbaaSClassifierBuilder(null).build());
        DataSource serviceDataSource = servicePostgresDb.getConnectionProperties().getDataSource();
        // use your custom classifier builder when needed
        PostgresDatabase postgresDbWithCustomClassifier = databasePool.getOrCreateDatabase(PostgresDBType.INSTANCE, myClassifierBuilder.build());
        DataSource anotherDataSource = postgresDbWithCustomClassifier.getConnectionProperties().getDataSource();

        // use these DataSources as you need

    }

    private DataSource createDataSource(PostgresDBConnection connectionProperties) {
        // create DataSource from connectionProperties
    }
    ```
4. To obtain `DataSource` objects use whether `DbaasPostgresProxyDataSource` class or `DatabasePool` instance directly 
    (both ways shown in example in step 3).  
 
