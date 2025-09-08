# DBaaS Client Postgres Starter

DBaaS Client starter for Spring-based applications working with PostgreSQL. 

This module can be included as a dependency directly to your microservice. 

- [Requirements](#requirements)
- [How to use](#how-to-use)
  * [Steps to set up library](#steps-to-set-up-library)
  * [Postgres creation params](#postgres-creation-params)
  * [Access to several databases](#access-to-several-databases)
  * [SSL/TLS support](#ssltls-support)
  * [Datasource builder](#dbaas-datasource-builder)
      * [Usage](#usage)
  * [DataSource metrics](#datasource-metrics)
- [Sample microservice](#sample-microservice)

## How to use
### Steps to set up library
1. Add dependencies to your pom.xml: 
```xml
 <dependency>
     <groupId>com.netcracker.cloud</groupId>
     <artifactId>dbaas-client-postgres-starter</artifactId>
     <version>${dbaas-client-postgres-starter.version}</version>
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
        
Where `${dbaas-client-postgres-starter.version}` is the desired library version. 

**Important!** Omit tag `version` if you are using [qubership-spring-boot-starter-parent](https://github.com/Netcracker/qubership-core-springboot-starter), 
in such case version of `dbaas-client-postgres-starter` will be resolved automatically.  

2. Configure necessary postgres DataSource options according to [Dbaas Client Configuration - Postgres DataSource Options](/docs/dbaas-client-configuration.md).  

3. Add one of the following annotations to your main `Application` class or any Spring configuration class:   
    * `@EnableServiceDbaasPostgresql` - for working with only a service database   
    * `@EnableTenantDbaasPostgresql`  - for working with only tenant databases   
    * `@EnableDbaasPostgresql`        - for working with both service and tenant databases 

These annotations create service and tenant `datasource` beans.

#### Usage `@EnableServiceDbaasPostgresql` annotation
This annotation creates `only service datasource` with `servicePostgresDatasource` qualifier. You can inject this datasource by the following way:
```java
import static com.netcracker.cloud.dbaas.client.config.DbaasPostgresConfiguration.SERVICE_POSTGRES_DATASOURCE;

@Autowired
@Qualifier(SERVICE_POSTGRES_DATASOURCE)
private DataSource serviceDatasource;
```

Besides that, you can use `spring data JPA` with a DbaaS solution. For it, you just need to put `@EnableJpaRepositories` over one of your configuration file.

#### Usage `@EnableTenantDbaasPostgresql` annotation
This annotation creates `only tenant datasource` with a `tenantPostgresDatasource` qualifier. You can inject this datasource by the following way:
```java
import static com.netcracker.cloud.dbaas.client.config.DbaasPostgresConfiguration.TENANT_POSTGRES_DATASOURCE;

@Autowired
@Qualifier(TENANT_POSTGRES_DATASOURCE)
private DataSource dbaasPostgresDataSource;
```

Besides that, you can use `spring data JPA` with a database solution. For it, you just need to put `@EnableJpaRepositories` over one of your configuration file.

#### Usage `@EnableDbaasPostgresql` annotation
This annotation allows to work with service and tenant databases at the same microservice. The annotation brings configurations that build `service and tenant` specific 
datasources.  
Service datasource has `servicePostgresDatasource` qualifier and tenant one has `tenantPostgresDatasource` qualifier. You may inject them by the following way: 

```java
import static com.netcracker.cloud.dbaas.client.config.DbaasPostgresConfiguration.*;

@Autowired
@Qualifier(SERVICE_POSTGRES_DATASOURCE) // it's a primary bean by default
private DataSource serviceDatasource;

@Autowired
@Qualifier(TENANT_POSTGRES_DATASOURCE)
private DataSource tenantDatasource;
```

You may use `spring data JPA` without additional configurations with only service database or tenant database. By default, it's a `service database`.
In order to use `spring data JPA`, you should put `@EnableJpaRepositories` over one spring configuration class. 
So, by this way you can work with a service database using `spring data JPA` and with tenant database using a raw datasource (or you may build `JdbcTemplate`) at the same time. 


If you want to use `spring data JPA` for working with a tenant database, then you need to specify a configuration property:
```text
dbaas.api.postgres.datasource.primary-bean=tenant
``` 
This configuration changes a primary bean from `service database (servicePostgresDatasource)` to `tenant database (tenantPostgresDatasource)`. Spring uses a primary bean
to build all beans that require a `DataSource` (e.g. `EntityManager`).  
If you want to use `spring data JPA` with both service and tenant database, then you need to put `@EnableJpaRepositories` and 
provide your own configurations (`entityManagerFactory`, `transactionManager`) for one of database type.  
You can find more of configuring both tenant-aware and service-aware `Spring Data Repositories` in [Sample microservice](#sample-microservice) section.

4. Due to spring-boot 2.7 issue with circular dependencies https://github.com/spring-projects/spring-boot/issues/31405 add the property to your service .yml file:
```yaml
spring:
  autoconfigure:
    exclude:
      - org.springframework.boot.actuate.autoconfigure.metrics.jdbc.DataSourcePoolMetricsAutoConfiguration
``` 

### Postgres creation params

To set a prefix to the database name, you need to set next property in application.properties/application.yml file:
```
dbaas.api.postgres.db-prefix=some-prefix
```
Property, that allows to configure connection user role for both service and tenant database. Default is "admin".
```
dbaas.api.postgres.runtime-user-role=some-role
```
To set pg-extensions to database settings, you need to set next property in application.properties/application.yml file:
```
dbaas.api.postgres.database-settings.pg-extensions=some-value1,some-value2
```
To set pg-extensions to tenant based database settings, you need to set next property in application.properties/application.yml file:
```
dbaas.api.postgres.tenant.database-settings.pg-extensions=some-value1,some-value2
```
To set pg-extensions to service based database settings, you need to set next property in application.properties/application.yml file:
```
dbaas.api.postgres.service.database-settings.pg-extensions=some-value1,some-value2
```

### Access to several databases
To be able to connect multiple PostgreSQL databases to one microservice, you need to follow these steps:
1. To connect to another service database, you need to add the next bean in configuration class:
```java
@Bean(name = {SECOND_SERVICE_POSTGRES_DATASOURCE})
    public DataSource servicePostgresDatasource(DatabasePool databasePool,
                                                DbaasClassifierFactory classifierFactory) {
        PostgresSettings databaseSettings = new PostgresSettings(postgresDbaasApiProperties.getDatabaseSettings(DbaasApiProperties.DbScope.SERVICE));

        DatabaseConfig databaseConfig = DatabaseConfig.builder()
                .userRole("admin")
                .dbNamePrefix("some-prefix")
                .databaseSettings(databaseSettings)
                .build();
        return new DbaasPostgresProxyDataSource(databasePool, classifierFactory.newServiceClassifierBuilder().withProperty("some-new-property", "some-new-value"), databaseConfig);
    }
```
2. In `classifierFactory.newServiceClassifierBuilder().withProperty("some-new-property", "some-new-value")` specify new classifier field and value. For example, it's may be "dbId" field. A new classifier is needed to distinguish between databases.
3. In `DatabaseConfig.builder().userRole("admin").dbNamePrefix("some-prefix").databaseSettings(databaseSettings)` you should specify the user role (default is admin role) for declarative database connection and database prefix and database settings based on service database.

After these steps you can get the connection to second database:
```java
@Qualifier("SECOND_SERVICE_POSTGRES_DATASOURCE")
DataSource secondDataSource;

PostgresDatabase database = secondDatasource.getDatabase();
//...
```

#### Getting access to another service's database

For getting connection to another microservice's database, you should specify database classifier.
The "microserviceName" field is determined from `cloud.microservice.name` property by default. So, you can specify a different microservice name to classifier through `.withProperty("microserviceName", "different-name")`.

### SSL/TLS support

This library supports work with secured connections to postgresql. Connection will be secured if TLS mode is enabled in
postgresql-adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

#### Physical database switching

> These parameters are given as an example. For reliable information, check adapter's documentation: https://github.com/Netcracker/pgskipper-operator/blob/main/docs/public/installation.md#tls

To enable TLS support in physical database redeploy postgresql with mandatory parameters
```yaml
tls.enabled=true;
```

In case of using cert-manager as certificates source add extra parameters
```yaml
ISSUER_NAME=<cluster issuer name>;
tls.certificateSecretName=pg-cert
tls.generateCerts.enabled=true
tls.generateCerts.clusterIssuerName=<cluster issuer name>
```

ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

### DbaaS datasource builder

`dbaas-client-postgres-starter` module provides `DbaasPostgresqlDatasourceBuilder` bean that provides a convenient and 
flexible way to configure and create a PostgreSQL datasource with various options that is integrated with DbaaS. 
It is especially useful for applications where you need to manage database connections efficiently or build datasources with different classifiers.  
Builder uses `HikariCP` datasource implementation.

By using the following annotations: `@EnableServiceDbaasPostgresql`, `@EnableTenantDbaasPostgresql`,
and `@EnableDbaasPostgresql`,
the dbaas-client library provides the `DbaasPostgresqlDatasourceBuilder` bean along with other default datasources.
If you don't intend to use the default service and tenant datasources, you can choose the
dedicated `@EnableDbaasDatasourceBuilder`
annotation to create the DbaasPostgresqlDatasourceBuilder bean and build your own datasources.

#### Usage

To use this class, you need to inject instance of `DbaasPostgresqlDatasourceBuilder` and use the `Builder` class to
set various configuration options for the datasource. After configuring the datasource, you can call the `build()`
method to obtain a fully configured `DataSource` instance.

```java
@Autowired
private DbaasPostgresqlDatasourceBuilder dbaasDatasourceBuilder;

public void customDatasource() {
    DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("ro").build();
    DataSource dataSource = dbaasDatasourceBuilder.newBuilder(classifierBuilder)
            .withSchema("schemaName")
            .withDiscriminator("discriminatorValue")
            .withDatabaseConfig(databaseConfig)
            .withConnectionProperties(connPropertiesParam)
            .withFlyway(getFlywayRunner())
            .withRoReplica()
            .build();
}

private static FlywayRunner getFlywayRunner() {
    return context -> {
        Flyway flyway = Flyway.configure()
                .dataSource(context.getDataSource())
                .baselineOnMigrate(true)
                .locations("classpath:db/migration/postgresql")
                .load();
        flyway.migrate();
    };
}
```

#### Methods

These methods allow you to configure and customize the behavior of the datasource created by the `DbaasPostgresqlDatasourceBuilder`.

- `newBuilder(DbaaSChainClassifierBuilder classifierBuilder)`: Create a new builder instance with the specified `DbaaSChainClassifierBuilder` and return it. 
The most appropriate implementations are `ServiceDbaaSClassifierBuilder` and `TenantDbaaSClassifierBuilder`. 
`DbaaSChainClassifierBuilder` provides `withProperty` and `withCustomKey` methods that allow you to pass custom classifier keys.

- `withDatabaseConfig(DatabaseConfig databaseConfig)`: Set the `DatabaseConfig` instance for the datasource. 
`DatabaseConfig` allows you to configure properties related to the database, such as `dbNamePrefix`, `databaseSettings`, and `physicalDatabaseId`.

- `withConnectionProperties(Map<String, Object> connPropertiesParam)`: Set additional connection properties for the datasource. These properties are used to configure HikariConfig. 
In the library, we create a `new HikariConfig(props)` using the provided properties. If you have questions about how to configure a specific parameter, you should refer to the official documentation and code.

- `withSchema(String schema)`: Set the database schema name for the datasource. By default `public schema is created and used`.

- `withDiscriminator(String discriminator)`: Set the discriminator value for the dbaas pool cache. This field is an additional field in the databasePool cache. 
By default, the discriminator is constructed using userRole and schema. If you provide your own value, it will override the default value and be used in the DbaaS cache.

- `withFlyway(FlywayRunner provider)`: Set the `FlywayRunner` instance for the datasource. You can use this method to
  describe the Flyway migration process,
  and the builder will execute this process right after creating the datasource. 
The builder passes a context object as an input parameter, from which you can obtain the created datasource. 
If you have custom `PostConnectProcessor`, it will be executed after `FlywayRunner`.

- `withRoReplica()`: ReadOnly connection. Add this method for connect to a readOnly replica if you want to perform only read operations.
  By default, the connection will go to the master replica

- `build()`: Create and return a fully configured `DataSource` instance. This method creates a fully configured datasource based on the settings provided through the other methods.

### DataSource metrics

DataSource metrics are automatically provided if you have enabled any of the DBaaS postgres datasources and use spring autoconfiguration while having actuator dependency:
```xml
  <dependency>
      <groupId>org.springframework.boot</groupId>
      <artifactId>spring-boot-starter-actuator</artifactId>
  </dependency>
```

You can disable metrics if needed with the following configuration property:
```
dbaas.postgres.metrics.enabled=false
```

## Sample microservice
Sample microservice using DBaaS Client starter for postgres can be found  [here](../dbaas-client-sample-tests/postgres-sample-test). 

## Logical database provider
dbaas-client-postgress-strater supports work with DB created by client side(not from dbaas-aggregator). You can read more info [here](https://github.com/Netcracker/qubership-dbaas-client/blob/main/dbaas-client-java/dbaas-client-core/README.md#logical-database-provider-feature).
