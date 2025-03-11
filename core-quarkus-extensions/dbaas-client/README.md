# Quarkus DbaaS client extensions

Quarkus DbaaS client extensions provide opportunities to communicate with DbaaS, create or get
a logical database and initialize database clients, for instance postgresql datasource or mongo client.  
Additionally, all clients support multi-tenancy and can work with both service and tenant databases.  
**Note**: Your application should provide `cloud.microservice.name` and `cloud.microservice.namespace` properties.
Also, if your work with a tenant database then `TenantContext` must contain `tenantId`.

* [Common properties](#common-properties)
* [PostgreSQL client extension](#postgresql-client-extension)
    * [How to use DbaaS PostgreSQL extension](#how-to-use-dbaas-postgresql-extension)
    * [How to use with DataSource Panache](#how-to-use-with-datasource-panache)
    * [Flyway migration](#flyway-migration)
    * [How to use own classifier in PostgreSQL extension](#how-to-use-own-classifier-in-postgresql-extension-bean-overriding)
    * [BlueGreen static and versioned databases support](#bluegreen-static-and-versioned-databases-support)
    * [PostgreSQL client properties](#postgresql-client-properties)
    * [Supported postgreSQl client features](#supported-postgresql-client-features)
    * [DbaasDataSource Builder](#dbaasdatasource-builder)
    * [DataSource Metrics](#datasource-metrics)
* [Cassandra client extension](#cassandra-client-extension)
    * [How to use DbaaS Cassandra extension](#how-to-use-dbaas-cassandra-extension)
    * [Cassandra client properties](#cassandra-client-properties)
    * [Cassandra Metrics](#cassandra-metrics)
    * [Cassandra Migration](#cassandra-migration)
* [MongoDB client extension](#mongodb-client-extension)
  * [How to use DbaaS MongoDB extension](#how-to-use-dbaas-mongodb-extension)
  * [How to use with Mongo Panache](#how-to-use-with-mongo-panache)
  * [How to use own classifier in MongoDB extension](#how-to-use-own-classifier-in-mongodb-extension)
  * [MongoDB client properties](#mongodb-client-properties)
* [Opensearch client extension](#opensearch-client-extension)
  * [How to use DbaaS Opensearch extension](#how-to-use-dbaas-opensearach-extension)
  * [Opensearch client properties](#opensearach-client-properties)
  * [Opensearch connection properties configuration](#opensearch-connection-properties-configuration)
  * [Opensearch Metrics](#opensearch-metrics)
* [SSL/TLS support in libraries](#ssltls-support-in-libraries)
* [TestUtils](#testutils)

## Common properties

| Property                    | Description                                                                                  | Default value                | Status                                          |
| --------------------------- | -------------------------------------------------------------------------------------------- | ---------------------------- | ----------------------------------------------- |
| quarkus.dbaas.api.agent.url | Sets the URL ("[schema]\://[host]\:[port]") of the dbaas-agent                               |   http://dbaas-agent:8080    | since 3.0.0. Property name was changed in 3.0.0 |
| cloud.microservice.name      | Sets an microsevice name and it's a part of database classifier. This parameter is required. | `no default value. Required` | since 0.9.0                                     |
| cloud.microservice.namespace | Sets and namespace where microservice is running. Part of classifier and required property.  | `no default value. Required` | since 0.9.0                                     |

## PostgreSQL client extension

Quarkus DbaaS PostgreSQL extension creates logical databases and initializes postgresql datasource clients.
Also, this extension is integrated with the flyway migration tool and supports multi-datasources work.
Pay attention, that logical database is created lazily and will be created when you first access the database.
In order to start working with this extension you should add to a POM the following dependency:
```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>dbaas-datasource-postgresql</artifactId>
            <version>{version}</version>
        </dependency>
```
where the last version can be found by URL: https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/dbaas-datasource-postgresql/.  
After the above dependency is added, you can start working through named beans or using Panache.

### How to use DbaaS PostgreSQL extension

Quarkus DbaaS postgreSQL extension creates datasources for two types of logical db: service or tenant.
To use `service database` you should inject the following named bean:
```java
    @Inject
    @Named(SERVICE_DATASOURCE)
    AgroalDataSource serviceAgroalDataSource;

    public void hello() throws SQLException {
            serviceAgroalDataSource.getConnection();
    }
```

In order to use `tenant database` you should inject the following named bean:

```java
    @Inject
    @Named(TENANT_DATASOURCE)
    AgroalDataSource tenantAgroalDataSource;

    public void hello() throws SQLException {
            tenantAgroalDataSource.getConnection();
    }

```

### How to use with DataSource Panache

Quarkus DbaaS postgresql extension allows to use Quarkus Panache but with only one type of logical database: service or tenant.  
So, the first step you need to do is to define which database (tenant or service) will be used by Panache and
set the bellow property in an application file:

```text
quarkus.dbaas.datasource.main-type=service
``` 

By default, the property has a `tenant` value.

After it, you should make default steps: create an entity, create repository interface that is
extended `PanacheRepositoryBase`
and inject your repository to your classes. You can see an example in our quarkus
quickstarter: https://git.qubership.org/DEMO.Platform_Researches/quarkus-quickstart.    
Sometimes, you also need to use the other kind of database, for example, your use two types of logical database in your
microservice at the same time. In this case, you should inject a named bean and use it for database operations. In other
words, you can define `quarkus.dbaas.datasource.main-type=service` and use Panache with serviceDb and
use `@Named(TENANT_DATASOURCE) AgroalDataSource tenantAgroalDataSource`
where you need tenant database or `quarkus.dbaas.datasource.main-type=tenant` for using tenantDb with Panache and
`@Named(SERVICE_DATASOURCE) AgroalDataSource serviceAgroalDataSource` where you need serviceDb.

### Flyway migration

Quarkus DbaaS postgresql extension brings out of the box Flyway migration tools. Migration process will perform if there
are migration files by default path: `db/migration`. By default, migration is lazy and is performed during the first
request to database.  
In some development cases, it can be useful to perform clean and migration during start time, in this case set the
property `quarkus.dbaas.flyway.clean-and-migrate-at-start` to true, for instance:

```text
quarkus.dbaas.flyway.clean-and-migrate-at-start=true
```

If you want to change the default migration implementation you should implement `MigrationService` interface and
set `quarkus.dbaas.flyway.clean-and-migrate-at-start=false` :

```
@Priority(1)
@Alternative
@Dependent
public class CustomMigrationService implements MigrationService {
    @Override
    public int migrate(DataSource dataSource) {
      ...
   }
}
```

If you are using Flyway, then it's required to put @Transactional annotation with Transactional.TxType.NOT_SUPPORTED
parameter value on migrate function. In general, if migration framework invokes setAutoCommit(true) on
javax.sql.Connection object, then it's required to suspend transaction (so migration framework must provide correct
behaviour for migrate and rollback scenarios).

Out of the box dbaas provides some configuration properties to configure flyway. If it is not enough and you can write you own flyway customizer.  
In order to do this you need to implement `FlywayConfigurationCustomizer` interface that has a `void customize(FluentConfiguration configure)` method.
`FluentConfiguration` object allows to configure flyway instance and you can set any properties that you want except `datasource`. 
For example: you want to set baselineVersion:

```
@ApplicationScoped
public class FlywayConfigurationCustomizerCustomImpl implements FlywayConfigurationCustomizer {

    @Override
    public void customize(FluentConfiguration configure) {
        configure.baselineVersion(coreFlywayConfig.baselineVersion);
    }
```

### How to use own classifier in PostgreSQL extension (Bean overriding)

If you want to work with your own classifier, you have to write your own `DbaaSClassifierBuilder` implementation.
In `build()` function you can add your own fields to classifier.

```
public class MyOwnClassifierBuilder implements DbaaSClassifierBuilder {

    @Override
    public DbaasDbClassifier build() {
        ...
    }
}
```

And you should create a class with beans configuration where you pass your `DbaaSClassifierBuilder` implementation.

```
public class Config {
    @Produces
    @Named(SERVICE_DATASOURCE) <-- name of overrided bean, must be SERVICE_DATASOURCE or TENANT_DATASOURCE.
    public AgroalDataSource getDbaasServiceDataSource(@NotNull DbaaSPostgresDbCreationService dataSourceCreationService) {
        return new DbaaSDataSource(new MyOwnClassifierBuilder(), dataSourceCreationService);
    }
}
```

Also, you can implement `AgroalDataSource` and use your own implementation instead of `DbaaSDataSource`.

### BlueGreen static and versioned databases support

This part of the documentation concerns the operation of the service in BlueGreen mode. In this mode each microservice
may have databases of two types: `static` (which contains business data) and `versioned` (which contains some configuration data).
On client level these databases differs with classifier. To split databases user have to define separate datasources for versioned database classifier.

Example of classifier for buisness database:
```json
classifier: {
    "scope": "service",
    "microserviceName": "test-name",
    "namespace": "test-namespace"
}
```

Example of classifier for configuration:
```json
classifier: {
    "scope": "service",
    "microserviceName": "test-name",
    "namespace": "test-namespace",
    "customKeys":{
        "logicalDbName": "configs" 
    }
}
```

For work with business database user may operate with ordinary SERVICE_DATASOURCE and TENANT_DATASOURCE (see section [How to use DbaaS PostgreSQL extension](#how-to-use-dbaas-postgresql-extension)).
For work with configuration database user have to define extra custom datasource. In below example "configs" is a logicalDbName which will be added to classifier.

```java
    @Produces
    @Named("configurationDS")
    public AgroalDataSource getDbaasConfigurationDataSource(@NotNull DbaaSPostgresDbCreationService dataSourceCreationService) {
        return new DbaaSDataSource(new ServiceClassifierBuilder().withCustomKey("logicalDbName", "configs"), dataSourceCreationService);
    }
```

Datasources which are bound to classifiers with `logicalDbName` (so datasource for configuration databases) may be configured 
via `application.yml` parameters. See sections [Datasource configuration for versioned databases](#datasource-configuration-for-versioned-databases)
and [Flyway configuration for versioned databases](#flyway-configuration-for-versioned-databases)

### PostgreSQL client properties

For configuring extension you should use [common properties](#common-properties) and below Quarkus DbaaS Postgresql specific properties:

## Datasource configuration

| Property                                                             | Description                                                                                                                                                                                                          | Default value                                           | Status                                          |
|----------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|-------------------------------------------------|
| quarkus.dbaas.datasource.main-type                                   | Define either `tenant` or `service` logical database will be used by Panache                                                                                                                                         | tenant                                                  | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.datasource.jdbc.max-size                               | Database maximal pool size                                                                                                                                                                                           | 5                                                       | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.datasource.jdbc.min-size                               | Database minimal pool size                                                                                                                                                                                           | 0                                                       | since 2.1.25                                    |
| quarkus.dbaas.datasource.jdbc.initial-size                           | Database initial pool size                                                                                                                                                                                           | 0                                                       | since 2.1.25                                    |
| quarkus.dbaas.datasource.jdbc-properties                             | Configurable Map with jdbc properties. Default value terminates any session with an open transaction that has been idle for longer than the specified duration in milliseconds. You have the ability to override it. | options=-c idle-in-transaction-session-timeout=28800000 | since 2.1.14                                    |
| quarkus.dbaas.datasource.jdbc.background-validation-interval.seconds | The interval at which we validate idle connections in the background. Set to 0 to disable background validation.                                                                                                     | 120                                                     | since 0.10.8                                    |
| quarkus.dbaas.datasource.jdbc.idle-reap-interval.seconds             | The interval at which we try to remove idle connections. This is analogue of quarkus "idle-removal-interval" setting.                                                                                                | 0.5                                                     | since 6.2.3                                     |
| quarkus.dbaas.datasource.jdbc.idle-removal-interval.seconds          | The interval at which we try to validate idle connections. This is analogue of quarkus "foreground-validation-interval" setting.                                                                                     | 0.5                                                     | since 0.10.8                                    |
| quarkus.dbaas.datasource.jdbc.acquisition-timeout.seconds            | The timeout before cancelling the acquisition of a new connection                                                                                                                                                    | 30                                                      | since 0.10.8                                    |
| quarkus.dbaas.datasource.jdbc.respond-time-to-drop.seconds           | The validation response time during which the connection is considered valid                                                                                                                                         | 5                                                       | since 0.10.8                                    |
| quarkus.dbaas.datasource.jdbc.leak-detection-interval.seconds        | The interval at which we check for connection leaks.                                                                                                                                                                 | 0                                                       | since 0.10.8                                    |
| quarkus.dbaas.datasource.jdbc.autocommit                             | The value of the auto-commit property on the connection.                                                                                                                                                             | true                                                    | since 0.10.13                                   |
| quarkus.dbaas.datasource.xa                                          | Use XA driver for all datasources                                                                                                                                                                                    | false                                                   | since 5.6.0                                     |

## Datasource configuration for versioned databases

| Property                                                                                | Description                                                                                                                                                                                                                                                                                  | Default value                                           | Status      |
|-----------------------------------------------------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|---------------------------------------------------------|-------------|
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc-properties                             | Configurable Map with jdbc properties for datasource with field "logicalDbName"=<logicalDbName> in classifier. Default value terminates any session with an open transaction that has been idle for longer than the specified duration in milliseconds. You have the ability to override it. | options=-c idle-in-transaction-session-timeout=28800000 | since 5.1.7 |
| quarkus.dbaas.datasources.\<logicalDbName\>.xa                                          | Use XA driver for the datasource for the specified logical DB                                                                                                                                                                                                                                | -                                                       | since 5.6.0 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.max-size                               | Database maximal pool size for the datasource for the specified logical DB                                                                                                                                                                                                                   | 5                                                       | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.min-size                               | Database minimal pool size for the datasource for the specified logical DB                                                                                                                                                                                                                   | 0                                                       | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.intial-size                            | Database initial pool size for the datasource for the specified logical DB                                                                                                                                                                                                                   | 0                                                       | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.background-validation-interval.seconds | The interval at which we validate idle connections in the background for the datasource for the specified logical DB. Set to 0 to disable background validation.                                                                                                                             | 120                                                     | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.idle-reap-interval.seconds             | The interval at which we try to remove idle connections for the datasource for the specified logical DB. This is analogue of quarkus "idle-removal-interval" setting.                                                                                                                        | 0.5                                                     | since 6.2.3 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.idle-removal-interval.seconds          | The interval at which we try to validate idle connections for the datasource for the specified logical DB. This is analogue of quarkus "foreground-validation-interval" setting.                                                                                                             | 0.5                                                     | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.acquisition-timeout.seconds            | The timeout before cancelling the acquisition of a new connection for the datasource for the specified logical DB                                                                                                                                                                            | 30                                                      | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.respond-time-to-drop.second            | The validation response time for the datasource for the specified logical DB during which the connection is considered valid                                                                                                                                                                 | 5                                                       | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.leak-detection-interval.seconds        | The interval at which we check for connection leaks for the datasource for the specified logical DB.                                                                                                                                                                                         | 0                                                       | since 6.2.2 |
| quarkus.dbaas.datasources.\<logicalDbName\>.jdbc.autocommit                             | The value of the auto-commit property on the connection for the datasource for the specified logical DB.                                                                                                                                                                                     | true                                                    | since 6.2.2 |



## Flyway configuration

| Property                                            | Description                                                                                                                                                                                                                                                                                                                                                                                                                                      | Default value    | Status       |
|-----------------------------------------------------|--------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------|--------------|
| quarkus.dbaas.flyway.clean-and-migrate-at-start      | Performs cleaning existed tables and doing migration during application start time.                                                                                                                                                                                                                                                                                                                                                              | false            | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.location                        | Comma-separated list of locations to scan recursively for migrations. Can contain both  SQL and Java-based migrations files.                                                                                                                                                                                                                                                                                                                     | db/migration     | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.connect-retries                 | The maximum number of retries when attempting to connect to the database.                                                                                                                                                                                                                                                                                                                                                                        | 0                | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.table                           | The name of Flyway’s history table.                                                                                                                                                                                                                                                                                                                                                                                                              | `none. Optional` | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.sql-migration-prefix            | The file name prefix for versioned SQL migrations.                                                                                                                                                                                                                                                                                                                                                                                               | `none. Optional` | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.repeatable-sql-migration-prefix | The file name prefix for repeatable SQL migrations.                                                                                                                                                                                                                                                                                                                                                                                              | `none. Optional` | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.baseline-on-migrate             | Enable the creation of the history table if it does not exist already.                                                                                                                                                                                                                                                                                                                                                                           | true             | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.baseline-version                | The initial baseline version.                                                                                                                                                                                                                                                                                                                                                                                                                    | 1                | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.baseline-description            | The description to tag an existing schema with when executing baseline.                                                                                                                                                                                                                                                                                                                                                                          | `none. Optional` | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.validate-on-migrate             | Whether to automatically call validate when performing a migration.                                                                                                                                                                                                                                                                                                                                                                              | true             | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.create-schemas                  | Whether Flyway should attempt to create the schemas specified in the schemas property.                                                                                                                                                                                                                                                                                                                                                           | true             | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.dbaas.flyway.out-of-order                    | Allows migrations to be run "out of order".                                                                                                                                                                                                                                                                                                                                                                                                      | false            | since 3.0.0. Property name was changed in 3.0.0   |
| quarkus.core.flyway.ignore-missing-migrations        | Ignore missing migrations when reading the history table. When set to true migrations from older versions present in the history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default) the validation step will fail.                                                                                                                                                                 | false            | removed since 3.1.0                               |
| quarkus.core.flyway.ignore-future-migrations         | Ignore future migrations when reading the schema history table. When set to true migrations from newer versions presents in the history table but absent in the configured locations will be ignored (and logged as a warning), when false (the default) the validation step will fail. This is useful for situations where one must be able to redeploy an older version of the application after the database has been migrated by a newer one | true             | removed since 3.1.0                               |
| quarkus.dbaas.flyway.ignore-migration-patterns       | Ignore migrations during validate and repair operations according to a given list of patterns when reading the history table. Patterns are of the form type:status with * matching type or status. The detailed description can be found in [flyway library documentation](https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns).                                                                                 | *:future         | since 3.1.0. Property name was changed in 3.1.0   |
| quarkus.dbaas.flyway.clean-disabled                  | Whether Flyway allows clean operation.                                                                                                                                                                                                                                                                                                                                                                                                           | false            | since 3.4.0. Property name was changed in 3.4.0   |

## Flyway configuration for versioned databases

| Property                                                                           | Description                                                                                                                                                                                                                                                                                                                                                      | Default value                            | Status      |
|------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|------------------------------------------|-------------|
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.clean-and-migrate-at-start      | Performs cleaning existed tables and doing migration during application start time.                                                                                                                                                                                                                                                                              | false                                    | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.location                        | Comma-separated list of locations to scan recursively for migrations. Can contain both  SQL and Java-based migrations files.                                                                                                                                                                                                                                     | versioned/db/migration/\<logicalDbName\> | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.connect-retries                 | The maximum number of retries when attempting to connect to the database.                                                                                                                                                                                                                                                                                        | 0                                        | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.table                           | The name of Flyway’s history table.                                                                                                                                                                                                                                                                                                                              | `none. Optional`                         | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.sql-migration-prefix            | The file name prefix for versioned SQL migrations.                                                                                                                                                                                                                                                                                                               | `none. Optional`                         | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.repeatable-sql-migration-prefix | The file name prefix for repeatable SQL migrations.                                                                                                                                                                                                                                                                                                              | `none. Optional`                         | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.baseline-on-migrate             | Enable the creation of the history table if it does not exist already.                                                                                                                                                                                                                                                                                           | true                                     | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.baseline-version                | The initial baseline version.                                                                                                                                                                                                                                                                                                                                    | 1                                        | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.baseline-description            | The description to tag an existing schema with when executing baseline.                                                                                                                                                                                                                                                                                          | `none. Optional`                         | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.validate-on-migrate             | Whether to automatically call validate when performing a migration.                                                                                                                                                                                                                                                                                              | true                                     | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.create-schemas                  | Whether Flyway should attempt to create the schemas specified in the schemas property.                                                                                                                                                                                                                                                                           | true                                     | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.out-of-order                    | Allows migrations to be run "out of order".                                                                                                                                                                                                                                                                                                                      | false                                    | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.ignore-migration-patterns       | Ignore migrations during validate and repair operations according to a given list of patterns when reading the history table. Patterns are of the form type:status with * matching type or status. The detailed description can be found in [flyway library documentation](https://flywaydb.org/documentation/configuration/parameters/ignoreMigrationPatterns). | *:future                                 | since 5.1.7 |
| quarkus.dbaas.flyway.datasources.\<logicalDbName\>.clean-disabled                  | Whether Flyway allows clean operation.                                                                                                                                                                                                                                                                                                                           | false                                    | since 5.1.7 |


**Postgresql creation params**                                         
This group of params allows to configure databases with different types. It is possible to set settings for:
* service database, e.g. `quarkus.dbaas.postgresql.api.service.physical-database-id`
* all tenant databases   `quarkus.dbaas.postgresql.api.tenant.physical-database-id`
* specific tenant database with id `quarkus.dbaas.postgresql.api.tenant.<tenant-id>.physical-database-id`

| Property                                                                        | Description                                                                       | Default value     | Status                                          |
|---------------------------------------------------------------------------------|-----------------------------------------------------------------------------------|-------------------|-------------------------------------------------|
| quarkus.dbaas.postgresql.api.[service \|\| tenant \|\| tenant.\<tenant-id\>].database-settings     | Allows adding pgExtensions to databases for functional extension.                   | `none. Optional`  | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.postgresql.api.[service \|\| tenant \|\| tenant.\<tenant-id\>].physical-database-id | Allows connect database with specific physical database.                          | `none. Optional`  | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.postgresql.api.runtime-user-role                                  | Allows configure connection user role for both service and tenant database        | `admin. Optional` | since 3.0.0                                     |
| quarkus.dbaas.postgresql.api.db-prefix                                          | Allows configure database name prefix for both service and tenant database creation | `none. Optional`  | since 3.3.0                                     |

### Supported postgresql client features

List of supported features and their description can be found on a special page: [list supported features](./docs/postgresql/List%20supported%20features.md)

### DbaasDataSource Builder

`dbaas-datasource-postgresql` module provides `DbaasQuarkusPostgresqlDatasourceBuilder` bean that provides a convenient and
flexible way to configure and create a PostgreSQL datasource with various options that is integrated with DbaaS.
It is especially useful for applications where you need to manage database connections efficiently or build datasources with different classifiers.  
Builder uses `AgroalDataSource` datasource implementation. Bean is supported out of the box.

To customize jdbc properties and pass them as customparams in builder, you can use properties. It will create the database pool configuration supporting below customize properties.
These properties set in DatasourceConnectorSettings as setConnPropertiesParam
Pass jdbc properties with this format "jdbc.<<propertykey>>" to customparams of Builder
Example: "jdbc.max-size"

| Property                                    | Description                                                                                                                      | Default value | status      |
|---------------------------------------------|----------------------------------------------------------------------------------------------------------------------------------|---------------|-------------|
| jdbc.max-size                               | Database maximal pool size                                                                                                       | 5             | since 6.2.2 |
| jdbc.min-size                               | Database minimal pool size                                                                                                       | 0             | since 6.2.2 |
| jdbc.initial-size                           | Database initial pool                                                                                                            | 0             | since 6.2.2 |
| jdbc.idle-reap-interval.seconds             | The interval at which we try to remove idle connections. This is analogue of quarkus "idle-removal-interval" setting.            | 0.5           | since 6.2.3 |     
| jdbc.idle-removal-interval.seconds          | The interval at which we try to validate idle connections. This is analogue of quarkus "foreground-validation-interval" setting. | 0.5           | since 6.2.2 |
| jdbc.acquisition-timeout.seconds            | The timeout before cancelling the acquisition of a new connection                                                                | 30            | since 6.2.2 |
| jdbc.respond-time-to-drop.seconds           | The validation response time during which the connection is considered valid                                                     | 5             | since 6.2.2 |                              
| jdbc.leak-detection-interval.seconds        | The interval at which we check for connection leaks.                                                                             | 0             | since 6.2.2 |                             
| jdbc.autocommit                             | The value of the auto-commit property on the connection.                                                                         | true          | since 6.2.2 |
| jdbc.background-validation-interval.seconds | The interval at which we validate idle connections in the background. Set to 0 to disable background validation.                 | 120           | since 6.2.2 |


### DataSource Metrics

`dbaas-datasource-postgresql` module provides integrations with micrometer metrics. If micrometer metrics are enabled in quarkus (by using property 'quarkus.micrometer.enabled')
then metrics for all postgresql datasources will be published in micrometer and exported at prometheus endpoint (if it is also enabled).

#### Usage

To use this class, you need to inject instance of `DbaasQuarkusPostgresqlDatasourceBuilder` and use the `Builder` class to
set various configuration options for the datasource. After configuring the datasource, you can call the `build()`
method to obtain a fully configured `DataSource` instance.

```java
@Autowired
private DbaasQuarkusPostgresqlDatasourceBuilder dbaasDatasourceBuilder;

public void customDatasource() {
    DatabaseConfig databaseConfig = DatabaseConfig.builder().userRole("ro").build();
    DataSource dataSource = dbaasDatasourceBuilder.newBuilder(classifierBuilder)
            .withSchema("schemaName")
            .withDiscriminator("discriminatorValue")
            .withDatabaseConfig(databaseConfig)
            .withConnectionProperties(connPropertiesParam)
            .withFlyway(getFlywayRunner())
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

- `newBuilder(DbaaSClassifierBuilder classifierBuilder)`: Create a new builder instance with the specified `DbaaSChainClassifierBuilder` and return it.
  The most appropriate implementations are `ServiceClassifierBuilder` and `TenantClassifierBuilder`.
  `DbaaSClassifierBuilder` provides `withProperty` and `withCustomKey` methods that allow you to pass custom classifier keys.

- `withDatabaseConfig(DatabaseConfig databaseConfig)`: Set the `DatabaseConfig` instance for the datasource.
  `DatabaseConfig` allows you to configure properties related to the database, such as `dbNamePrefix`, `databaseSettings`, and `physicalDatabaseId`.

- `withConnectionProperties(Map<String, Object> connPropertiesParam)`: Set additional connection properties for the datasource. These properties are used to configure AgroalDataSource.
  In the library, we create a `Properties(props)` using the provided properties. If you have questions about how to configure a specific parameter, you should refer to the official documentation and code.
  These properties will be merged with global configuration via application.yaml. `connPropertiesParam` will override global configuration in case of equal properties names.

- `withSchema(String schema)`: Set the database schema name for the datasource.

- `withDiscriminator(String discriminator)`: Set the discriminator value for the dbaas pool cache. This field is an additional field in the databasePool cache.
  By default, the discriminator is constructed using userRole and schema. If you provide your own value, it will override the default value and be used in the DbaaS cache.

- `withFlyway(FlywayRunner provider)`: Set the `FlywayRunner` instance for the datasource. You can use this method to
  describe the Flyway migration process,
  and the builder will execute this process right after creating the datasource.
  The builder passes a context object as an input parameter, from which you can obtain the created datasource.

- `build()`: Create and return a fully configured `DataSource` instance. This method creates a fully configured datasource based on the settings provided through the other methods.


## Cassandra client extension

Quarkus DbaaS Cassandra extension creates logical databases and initializes cassandra clients.
Also, this extension supports multi cassandra clients and allows to work with both tenant or service databases.
Pay attention, logical database is created lazily and it will be created when you first access the database.
In order to start working with this extension you should add to a POM the following dependency:
```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>dbaas-cassandra-client</artifactId>
            <version>{version}</version>
        </dependency>
```
where the last version can be found by URL: https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/dbaas-cassandra-client/.  
After the above dependency is added, you can start working through named beans.

### How to use DbaaS Cassandra extension

Quarkus DbaaS Cassandra extension creates cassandra clients for two types of logical db: service or tenant.
In order to use `service database` you should inject the following cassandra client named bean:
```java
    @Inject
    @Named(SERVICE_CASSANDRA_CLIENT)
    public CqlSession serviceDbaaSCassandraClient;

    public void hello() {
            ResultSet resultSet = serviceDbaaSCassandraClient.execute("select * from exampleTable where id='1'");
    }
```

In order to use `tenant database` you should inject the following cassandra client named bean:
```java
    @Inject
    @Named(TENANT_CASSANDRA_CLIENT)
    public CqlSession tenantDbaaSCassandraClient;

    public void hello() {
            ResultSet resultSet = tenantDbaaSCassandraClient.execute("select * from exampleTable where id='1'");
    }

```

### Cassandra client properties

For configuring extension you should use [common properties](#common-properties) and below Quarkus DbaaS Cassandra specific properties:

Some of these params allows to configure logical db with different types. It is possible to set settings for:

* service database, e.g. `quarkus.dbaas.cassandra.api.service.physical-database-id`
* all tenant databases, e.g. `quarkus.dbaas.cassandra.api.tenant.physical-database-id`
* specific tenant database with id,
  e.g. `quarkus.dbaas.cassandra.api.<tenant-id>.physical-database-id`

| Property                                                                          | Description                                                                                                                                    | Default value     | Status                                          |
|-----------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|-------------------------------------------------|
| quarkus.dbaas.cassandra.api.db-classifier                                         | Configures field "dbClassifier" in CassandraClassifier                                                                                         | default           | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.cassandra.api.[service\|tenant\|\<tenant-id\>].physical-database-id | Specify physicalDb in which logical db will be created                                                                                         | `none. Optional`  | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.cassandra.requestTimeoutMs                                          | Set up the timeout for requests within session with cassandra                                                                                  | default           | since 2.8.1                                     |
| quarkus.dbaas.cassandra.ssl                                                       | Enable SSL (TLS)                                                                                                                               | default           | since 2.8.1                                     |
| quarkus.dbaas.cassandra.truststorePath                                            | Trust store path with certificates for access to the resource worked over TLS                                                                  | default           | since 2.8.1                                     |
| quarkus.dbaas.cassandra.truststorePassword                                        | Password for the trust store path                                                                                                              | default           | since 2.8.1                                     |
| quarkus.dbaas.cassandra.api.db-prefix                                             | Allows configure database name prefix for both service and tenant database creation                                                            | `none. Optional`  | since 3.3.0                                     |
| quarkus.dbaas.cassandra.api.runtime-user-role                                     | Allows configure connection user role for both service and tenant database                                                                     | `admin. Optional` | since 4.2.0                                     |
| quarkus.dbaas.cassandra.ssl-hostname-validation                                   | Whether to require validation that the hostname of the server certificate's common name matches the hostname of the server being connected to. | true              | since 4.4.4                                     |
| quarkus.dbaas.cassandra.lb-slow-replica-avoidance                                 | Whether the slow replica avoidance should be enabled in the default LBP.                                                                       | true              | since 4.4.4                                     |

### Cassandra Metrics

`dbaas-cassandra-client` module provides integrations with micrometer metrics. If micrometer metrics are enabled in quarkus (by using property 'quarkus.micrometer.enabled')
then configured Cassandra metrics be published in micrometer and exported at prometheus endpoint (if it is also enabled).

Metrics support is enabled by default but require explicit configuration of which metrics you want to export. You can fully disable metrics if needed with the following configuration property:
```
quarkus.dbaas.cassandra.metrics.enabled=false
```

Session and node Cassandra metrics configuration is stored under the `quarkus.dbaas.cassandra.metrics.session` and `quarkus.dbaas.cassandra.metrics.node` keys respectively. They follow the same naming as in reference Cassandra configuration (https://docs.datastax.com/en/developer/java-driver/4.17/manual/core/configuration/reference/).

#### Sample metrics configuration
For example to enable `bytes-sent`, `bytes-received`, `connected-nodes`, `cql-requests` session metrics with additional configuration for `cql-requests` can be done with the following configuration in you application.properties file:
```yaml
quarkus.dbaas.cassandra.metrics.session.enabled=bytes-sent,bytes-received,connected-nodes,cql-requests
quarkus.dbaas.cassandra.metrics.session.cql-requests.highest-latency=10s
quarkus.dbaas.cassandra.metrics.session.cql-requests.lowest-latency=10ms
quarkus.dbaas.cassandra.metrics.session.cql-requests.significant-digits=2
quarkus.dbaas.cassandra.metrics.node.enabled=pool.open-connections,pool.available-streams,pool.in-flight
```

### Cassandra Migration

`dbaas-cassandra-client` module provides migration implementation that you can use. It will load migration scripts from the `db/migration/cassandra/versions` and execute them
You can disable this migration implementation by setting `quarkus.dbaas.cassandra.migration.enabled` property to `false` in your application configuration file.


| name                                                                      | default                                 | description                                                                                                                                                                     |
|---------------------------------------------------------------------------|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| quarkus.dbaas.cassandra.migration.schema-history-table-name                       | flyway_schema_history                   | name of the table to store schema version history                                                                                                                               |
| quarkus.dbaas.cassandra.migration.version.settings-resource-path                  | db/migration/settings.json              | resource path to get additional schema version settings. See also `SchemaVersionSettings` javadoc                                                                               |
| quarkus.dbaas.cassandra.migration.version.directory-path                          | db/migration/versions                   | directory path to scan for schema version resources                                                                                                                             |
| quarkus.dbaas.cassandra.migration.version.resource-name-pattern                   | "V(.+)__(.+)\\.(.+)"                    | pattern to get information about schema version from resource name, must contain ordered groups for 1 - version, 2 - description, 3 - resource type                             |
| quarkus.dbaas.cassandra.migration.template.definitions-resource-path              | db/migration/templating/definitions.ftl | resource path to get additional definitions to import into FreeMarker configuration and allow to be used in schema version scripts under fn namespace                           |
| quarkus.dbaas.cassandra.migration.lock.table-name                                 | schema_migration_lock                   | name of the table for migration locks holding                                                                                                                                   |
| quarkus.dbaas.cassandra.migration.lock.retry-delay                                | 5 000 (mills)                           | delay between attempts to acquire the lock                                                                                                                                      |
| quarkus.dbaas.cassandra.migration.lock.lock-lifetime                              | 60 000 (mills)                          | lock lifetime                                                                                                                                                                   |
| quarkus.dbaas.cassandra.migration.lock.extension-period                           | 5 000 (mills)                           | lock extension period                                                                                                                                                           |
| quarkus.dbaas.cassandra.migration.lock.extension-fail-retry-delay                 | 500 (mills)                             | lock extension delay after the extension failure. Will be applied until the extension success or lock-lifetime is passed.                                                       |
| quarkus.dbaas.cassandra.migration.schema-agreement.await-retry-delay              | 500 (mills)                             | retry delay for schema agreement await                                                                                                                                          |
| quarkus.dbaas.cassandra.migration.amazon-keyspaces.enabled                        | false                                   | true if Amazon Keyspaces is used instead of Cassandra                                                                                                                           |
| quarkus.dbaas.cassandra.migration.amazon-keyspaces.table-status-check.pre-delay   | 1 000 (mills)                           | preliminary delay before checking table status in system_schema_mcs.tables. Is required because Amazon Keyspaces updates the status in system_schema_mcs.tables asynchronously. |
| quarkus.dbaas.cassandra.migration.amazon-keyspaces.table-status-check.retry-delay | 500 (mills)                             | retry delay for checking expected table statuses in system_schema_mcs.tables                                                                                                    |

#### Templates usage

Migration feature also provides integration with Apache FreeMarker template engine and you can use templates written in the FreeMarker Template Language as your migration scripts.
They will be processed by the template engine and the result will be executed as a regular cql script.
Additionally when using templates `db/migration/cassandra/templating/functions.ftl` file will be automatcally [imported|https://freemarker.apache.org/docs/ref_directive_import.html] with `fn` hash. 
It can be used for the convenient storage of macroses for your templates.

#### Additional settings

It is possible to configure to ignore certain errors during migration scripts execution based on database response messages. Such configuration can be provided in the `db/migration/cassandra/settings.json` file
by mapping script version numbers to their specific configuration in the following format:
```json
{
  "{version}": {
    "ignoreErrorPatterns": [{message_regex}]
  }
}
```

For example:
```json
{
  "1.0": {
    "ignoreErrorPatterns": [
      ".*conflicts with an existing column.*",
      ".*already exists.*"
    ]
  }
}
```

#### Amazon Keyspaces

When using migration for Casandra databases managed by AWS it is required:
1. Set `quarkus.dbaas.cassandra.migration.amazon-keyspaces.enabled` property to `true` in your application configuration file;
2. Provide additional configuration in `db/migration/cassandra/settings.json` that describes DDL(UPDATE, CREATE, DROP) operations of the migration scripts in the following format:
```json
{
  "{version}": {
    "tableOperations": [
      {
        "tableName": "TABLE_NAME",
        "operationType": "OPERATION_TYPE"
      }
    ]
  }
}
```
where `TABLE_NAME` - is the name of the table that is being migrated and `OPERATION_TYPE` is DDL operation type (one of the following: UPDATE, CREATE, DROP).

If for example your `V1.0__schema_migration.cql` script contains creation of `sample_migration_table_1` then `settings.json` should have following:
```json
{
  "1.0": {
    "tableOperations": [
      {
        "tableName": "sample_migration_table_1",
        "operationType": "CREATE"
      }
    ]
  }
}
```

#### Custom migration implementation

It is possible to override migration implementation by registering your own bean implementing `org.qubership.cloud.dbaas.client.cassandra.migration.MigrationExecutor` interface.
In that case default migration logic provided by the library will be ignored.

## MongoDB client extension

Quarkus DbaaS MongoDB extension creates logical databases and initializes mongo clients.
Also, this extension supports multi mongo clients and allows to work with both tenant or service database.
Pay attention, logical database is created lazily and it will be created when you first access the database.
In order to start working with this extension you should add to a POM the following dependency:
```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>dbaas-mongo-client</artifactId>
            <version>{version}</version>
        </dependency>
```
where the last version can be found by URL: https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/dbaas-mongo-client/.  
After the above dependency is added, you can start working through named beans or using Mongo Panache.

### How to use DbaaS MongoDB extension

Quarkus DbaaS MongoDB extension creates mongo clients for two types of logical db: service or tenant.
In order to use `service database` you should inject the following mongo client named bean:
```java
    @Inject
    @Named(SERVICE_MONGO_CLIENT)
    public DbaaSMongoClient serviceDbaaSMongoClient;

    public void hello() {
            serviceDbaaSMongoClient.getDatabase();
    }
```

In order to use `tenant database` you should inject the following mongo client named bean:

```java
    @Inject
    @Named(TENANT_MONGO_CLIENT)
public DbaaSMongoClient tenantDbaaSMongoClient;

public void hello(){
        tenantDbaaSMongoClient.getDatabase();
        }

```  

### How to use with Mongo Panache

Quarkus DbaaS mongo extension allows to use Quarkus Panache but with only one type of logical database: service or
tenant.  
So, the first step you need to do is to define which database (tenant or service) will be used by Panache and set the
bellow property in an application file:

```text
quarkus.dbaas.mongodb.main-type=service
``` 

By default, the property has a `tenant` value.

After it, you should make default steps: create an entity, create repository interface that is
extended `PanacheRepositoryBase`
and inject your repository to your classes.     
Sometimes, you also need to use the other kind of database, for example, your use two types of logical database in your
microservice at the same time. In this case, you should inject a named bean and use it for database operations. In other
words, you can define `quarkus.dbaas.mongodb.main-type=service` and use Panache with serviceDb and
use `@Named(TENANT_DATASOURCE) AgroalDataSource tenantAgroalDataSource`
where you need tenant database or `quarkus.dbaas.mongodb.main-type=tenant` for using tenantDb with Panache and
`@Named(SERVICE_DATASOURCE) AgroalDataSource serviceAgroalDataSource` where you need serviceDb.

### How to use own classifier in MongoDB extension
Sometimes, you may want to use your not default classifier or use your personal mongo client implementation.
In these cases, you should override default DbaaS MongoDB beans.

At first, you have to write your own `DbaaSClassifierBuilder` implementation.
In `build()` function you can add your own fields to classifier.
```
public class MyOwnClassifier implements DbaaSClassifierBuilder {
    @Override
    public DbaasDbClassifier build() {
        ...
    }
}
```

At the end, you should create class with beans configuration.

```java
public class Config {
    @Produces
import jakarta.annotation.Priority;
import jakarta.enterprise.inject.Alternative;
    @Named(SERVICE_MONGO_CLIENT) <-- name of overrided bean, must be SERVICE_MONGO_CLIENT or TENANT_MONGO_CLIENT.
    public DbaaSMongoClient getCommonDbaasServiceMongoClient(@NotNull MongoClientCreation mongoClientCreation) {
        return new DbaaSMongoClient(new MyOwnClassifier(), mongoClientCreation);
    }
}
```

Also you can implement `MongoClient` and use your own implementation instead of `DbaaSMongoClient`.

### MongoDB client properties

For configuring extension you should use [common properties](#common-properties) and below Quarkus DbaaS MongoDB specific properties:

| Property                                  | Description                                                                         | Default value       | Status                                          |
|-------------------------------------------|-------------------------------------------------------------------------------------|---------------------|-------------------------------------------------|
| quarkus.dbaas.mongodb.db-classifier       | Configures field "dbClassifier" in MongoClassifier                                  | default             | since 3.0.0. Property name was changed in 3.0.0 |
| quarkus.dbaas.mongo.api.db-prefix         | Allows configure database name prefix for both service and tenant database creation | `none. Optional`    | since 3.3.0                                     |
| quarkus.dbaas.mongo.api.runtime-user-role | Allows configure connection user role for both service and tenant database          | `admin. Optional`   | since 3.8.0                                     |

## Opensearch client extension

Quarkus DbaaS Opensearach extension allows to manipulate Opensearch indices which were created via DbaaS and initializes
java Opensearch Rest High-Level clients. Also, this extension supports multi RestHighLevel clients and allows to work
with both tenant or service databases. Pay attention, logical database is created lazily and it will be created when you
first access the database.

In order to start working with this extension you should add to a POM the following dependencies:
```xml
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>dbaas-common</artifactId>
            <version>{version}</version>
        </dependency>
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>dbaas-opensearch-client</artifactId>
            <version>{version}</version>
        </dependency>
```
where the last version can be found by URL: https://artifactorycn.qubership.org/pd.saas.mvn/org/qubership/cloud/quarkus/dbaas-opensearch-client/.  
After the above dependency is added, you can start working through named beans.

### How to use DbaaS Opensearach extension

Quarkus DbaaS Opensearach extension creates opensearch clients for two types of logical db: service or tenant.

#### Attention

Use `client.normalize(UNIQ_INDEX_ALIAS_OR_TEMPLATE_NAME)`  when create request to opensearch.

#### Native client

If you want to use native `RestHighLevelClient`, it can be obtained by `serviceClient.getClient()`. But **pay
attention**
`you have to call serviceClient.getClient() method every time before each operation with RestHighLevelClient` in order
to provide some dbaas functionality such as password validation, reconnection, multi-tenancy and so on.

More info can be
found [here](https://git.qubership.org/PROD.Platform.Cloud_Core/dbaas-client/-/tree/main/dbaas-client-java/dbaas-client-opensearch-base#native-resthighlevelclient)

##### ServiceNative client

In order to use `service database` you should inject the following opensearch client named bean:

```java
    @Inject
    @Named(SERVICE_NATIVE_OPENSEARCH_CLIENT)
    DbaasOpensearchClient serviceClient;

public void hello(){
        IndexRequest updateIndexRequest=new IndexRequest(serviceClient.normalize(INDEX_NAME))
            .id("1")
            .source("Key","Value");
        try{
            IndexResponse indexResponse=serviceClient.getClient().index(updateIndexRequest,RequestOptions.DEFAULT);
            return"Creation response: "+indexResponse.status().getStatus();
        } catch(IOException e){
            e.printStackTrace();
        }
}
```

##### TenantNative client

In order to use `tenant database` you should inject the following opensearch client named bean:

```java
    @Inject
@Named(TENANT_NATIVE_OPENSEARCH_CLIENT)
    DbaasOpensearchClient tenantClient;

public void hello(){
        IndexRequest updateIndexRequest=new IndexRequest(tenantClient.normalize(INDEX_NAME))
            .id("1")
            .source("Key","Value");
        try{
            IndexResponse indexResponse=tenantClient.getClient().index(updateIndexRequest,RequestOptions.DEFAULT);
            return"Creation response: "+indexResponse.status().getStatus();
        } catch(IOException e){
            e.printStackTrace();
        }
}

```

DbaaS solution Information about supported methods and restrictions can be found
in https://git.qubership.org/PROD.Platform.Cloud_Core/dbaas-client/-/tree/main/dbaas-client-java/dbaas-client-opensearch-base
.

Service and tenant clients allow users to use API
from [OpenSearch Java client](https://opensearch.org/docs/latest/clients/java/).

### Opensearach client properties

For configuring extension you should use [common properties](#common-properties) and below Quarkus DbaaS Opensearach
specific properties:

Some of these params allows to configure logical db with different types. It is possible to set settings for:

* service database, e.g. `quarkus.dbaas.opensearch.api.service.physical-database-id`
* all tenant databases, e.g. `quarkus.dbaas.opensearch.api.tenant.physical-database-id`
* specific tenant database with id, e.g. `quarkus.dbaas.opensearch.api.<tenant-id>.physical-database-id`
* set prefix and delimiter to connect previously created index, alias or template

| Property                                                                                                   | Description                                                                                                                                                           | Default value     | Status                                          | example                                                                           |
|------------------------------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------------|-------------------|-------------------------------------------------|-----------------------------------------------------------------------------------|
| quarkus.dbaas.opensearch.api.[service &#124;&#124; tenant &#124;&#124; \<tenant-id\>].physical-database-id | Specify physcalDb in which logical db will be created                                                                                                                 | `none. Optional`  | since 3.0.0. Property name was changed in 3.0.0 |                                                                                   |
| `quarkus.dbaas.opensearch.api.service.prefix-config.prefix`                                                | Sets custom prefix for service database resource(index, alias, template)                                                                                              | `none. Optional`  | since 3.7.3.                                    | `quarkus.dbaas.opensearch.api.service.prefix-config.prefix=some-service-prefix`   |
| `quarkus.dbaas.opensearch.api.service.prefix-config.delimiter`                                             | Sets custom delimiter for service database                                                                                                                            | `_`               | since 3.7.3.                                    | `quarkus.dbaas.opensearch.api.service.prefix-config.delimiter=-`                  |
| `quarkus.dbaas.opensearch.api.tenant.prefix-config.prefix`                                                 | Sets custom prefix for all tenants database resource(index, alias, template). Put substring '{tenantId}' in prefix. This substring will be replaced to real tenantId. | `none. Optional`  | since 3.7.3.                                    | `quarkus.dbaas.opensearch.api.tenant.prefix-config.prefix=tenant-{tenantId}-some` |
| `quarkus.dbaas.opensearch.api.tenant.prefix-config.delimiter`                                              | Sets custom delimiter for all tenants database                                                                                                                        | `_`               | since 3.7.3.                                    | `quarkus.dbaas.opensearch.api.tenant.prefix-config.delimiter=--`                  |
| `quarkus.dbaas.opensearch.api.runtime-user-role`                                                           | Allows configure connection user role for both service and tenant database                                                                                            | `admin. Optional` | since 4.5.1                                     | `quarkus.dbaas.opensearch.api.runtime-user-role=dml`                              |  

### Opensearch connection properties configuration

You have an ability to set maxConnTotal and maxConnPerRoute in `application.yaml` file to set these settings in Apache Http Client.

| Property                                      | Description                                 | Default value    | Status       | Example                                          |
|-----------------------------------------------|---------------------------------------------|------------------|--------------|--------------------------------------------------|
| `quarkus.dbaas.opensearch.max-conn-total`     | Sets custom value for maxConnTotal field    | `none. Optional` | since 6.7.0. | `quarkus.dbaas.opensearch.max-conn-total=50`     |
| `quarkus.dbaas.opensearch.max-conn-per-route` | Sets custom value for maxConnPerRoute field | `none. Optional` | since 6.7.0. | `quarkus.dbaas.opensearch.max-conn-per-route=50` |



### Example `normalize` method with above configuration:

#### With serviceClient :

If you have `quarkus.dbaas.opensearch.api.service.prefix-config.prefix=some-service-prefix`
and `quarkus.dbaas.opensearch.api.service.prefix-config.delimiter=-`

```java
@Inject
@Named(SERVICE_NATIVE_OPENSEARCH_CLIENT)
private DbaasOpensearchClient serviceClient;

        GetRequest getRequest=new GetRequest(serviceClient.normalize("uniq_name"),"1"); // serviceClient.normalize returns 'some-service-prefix-uniq_name'
        boolean exists=serviceClient.getClient().exists(getRequest,RequestOptions.DEFAULT);
```

#### With tenantClient:

If you have `quarkus.dbaas.opensearch.api.tenant.prefix-config.prefix=tenant-{tenantId}-some`
, `quarkus.dbaas.opensearch.api.tenant.prefix-config.delimiter=--`, and tenantId is = `1234`

```java
@Inject
@Named(TENANT_NATIVE_OPENSEARCH_CLIENT)
private DbaasOpensearchClient tenantClient;

        GetRequest getRequest=new GetRequest(tenantClient.normalize("uniq_name"),"1"); // tenantClient.normalize returns 'tenant-1234-some--uniq_name'
        boolean exists=serviceClient.exists(getRequest,RequestOptions.DEFAULT);
```

### Opensearch Metrics

`dbaas-opensearch-client` module provides integrations with micrometer metrics. If micrometer metrics are enabled in quarkus (by using property 'quarkus.micrometer.enabled')
then configured Opensearch client metrics be published in micrometer and exported at prometheus endpoint (if it is also enabled).

Metrics support is enabled by default. You can fully disable metrics if needed with the following configuration property:
```
quarkus.dbaas.opensearch.metrics.enabled=false
```

#### Requests seconds metric
It is Opensearch client's metric called 'opensearch_client_requests_seconds'.
This metric is intended to record durations (in seconds) of requests from client to Opensearch.
Its metric configuration is stored under the `quarkus.dbaas.opensearch.metrics.requests-seconds` key.

#### Sample metrics configuration for Opensearch client

For example to enable 'opensearch_client_requests_seconds' metric with the following configuration in your application.yml file:

```yaml
# Properties for Opensearch client metrics
quarkus.dbaas.opensearch.metrics:
  # Default value: true
  enabled: true
  # Properties for 'opensearch_client_requests_seconds' metric
  requests-seconds:
    # Default value: true
    enabled: true
    # Possible values: SUMMARY or HISTOGRAM. Default value: SUMMARY
    type: SUMMARY
    # Default value: 1ms
    minimum-expected-value: 1ms
    # Default value: 30s
    maximum-expected-value: 30s
    # It matters only if type=SUMMARY
    # Property for list of Double numbers meaning quantiles
    # Default value: empty list
    quantiles: 0.25,0.5,0.75,0.95
    # It matters only if type=SUMMARY
    # Property for creating arbitrary amount of histogram buckets with values from 'minimum-expected-value' to 'maximum-expected-value' 
    # Default value: false
    quantile-histogram: false
    # It matters only if type=HISTOGRAM
    # Property for list of Duration instances meaning expected request durations in buckets
    # Default value: empty list
    histogram-buckets: 100ms,500ms,1000ms,2000ms,5000ms
```

## SSL/TLS support in libraries

Each module support work with secured connections. Connection will be secured if TLS mode is enabled in
corresponding adapter.

For correct work with secured connections, the library requires having a truststore with certificate.
It may be public cloud certificate, cert-manager's certificate or any type of certificates related to database.
We do not recommend use self-signed certificates. Instead, use default NC-CA.

To start using TLS feature user has to enable it on the physical database (adapter's) side and add certificate to service truststore.

### Physical database switching

> These parameters are given as an example. For reliable information, check each adapter's documentation
> * Postgresql: https://git.qubership.org/PROD.Platform.HA/postgres-operator/-/blob/main/docs/installation.md#tls
> * Cassandra: https://git.qubership.org/PROD.Platform.Databases/cassandra-operator/-/blob/main/docs/installation_guide.md#tls-encryption
> * Mongodb: https://git.qubership.org/PROD.Platform.Databases/mongodb-operator/-/blob/main/docs/installation_guide.md#tls-parameters
> * Opensearch: https://git.qubership.org/PROD.Platform.ElasticStack/opensearch-service/-/blob/main/documentation/installation-guide/encrypted-access/README.md


To enable TLS support in physical database redeploy database with parameters from column `Enable TLS`. 
In order to use TLS with cert-manager also add parameters from second column.

| Type       | Enable TLS                                                                            | Enable TLS with cert-manager                                                                                                                                    |
|------------|---------------------------------------------------------------------------------------|-----------------------------------------------------------------------------------------------------------------------------------------------------------------|
| postgresql | tls.enabled=true;                                                                     | ISSUER_NAME=<cluster issuer name>; tls.certificateSecretName=pg-cert; tls.generateCerts.enabled=true; tls.generateCerts.clusterIssuerName=<cluster issuer name> |
| cassandra  | tls.enabled=true;                                                                     | tls.generateCerts.enabled=true; tls.generateCerts.clusterIssuerName=<cluster issuer name>;                                                                      |
| mongodb    | tls.mode=requireTLS;                                                                  | tls.generateCerts.enabled=true; tls.generateCerts.clusterIssuerName=<cluster issuer name>                                                                       |
| opensearch | global.tls.enabled=true; opensearch.tls.enabled=true;  dbaasAdapter.tls.enabled=true; | global.tls.generateCerts.clusterIssuerName=<cluster issuer name>;                                                                                               |


ClusterIssuerName identifies which Certificate Authority cert-manager will use to issue a certificate.
It can be obtained from the person in charge of the cert-manager on the environment.

### Add certificate to service truststore

The platform deployer provides the bulk uploading of certificates to truststores.

In order to add required certificates to services truststore:
1. Check and get certificate which is used in database.
  * Postgresql: certificate is located in `Secrets` -> `pg-cert` -> `ca.crt`
  * Mongodb: certificate is located in `Secrets` -> `root-ca` -> `ca.crt`
  * Cassandra: certificate is located in `Secrets` -> `root-ca` -> `ca.crt`
  * Opensearch: certificate is located in `Secrets` -> `opensearch-rest-issuer-certs` -> `ca.crt`
2. TODO: certs update process

## TestUtils

This is a library that provides utility methods for your tests.

### Setting Up the Library
To set up the library, add the following dependencies to your `pom.xml`:
```xml
<dependency>
    <groupId>org.qubership.cloud.quarkus</groupId>
    <artifactId>dbaas-client-test</artifactId>
    <version>${dbaas-client-test.version}</version>
</dependency>
        
        <!-- Alternatively, you can add a dependency on cloud-core-quarkus-bom-publish -->

<dependencyManagement>
    <dependencies>
        <dependency>
            <groupId>org.qubership.cloud.quarkus</groupId>
            <artifactId>cloud-core-quarkus-bom-publish</artifactId>
            <version>${cloud.core.quarkus.version}</version>
            <type>pom</type>
            <scope>import</scope>
        </dependency>
    </dependencies>
</dependencyManagement>

<dependency>
    <groupId>org.qubership.cloud.quarkus</groupId>
    <artifactId>dbaas-client-test</artifactId>
</dependency>
```

You can find the available library versions [here](https://artifactorycn.qubership.org/pd.saas-release.mvn.group/org/qubership/cloud/quarkus/).

### DbaaS pool test utils
The `DatabasePoolTestUtils` class offers methods for tests that allow you to work with the Postgresql cache.

#### Initialization
To use `DatabasePoolTestUtils`, create an instance by passing a DbaaSPostgresDbCreationService. In your integration tests, you can inject a
`DbaaSPostgresDbCreationService` as a Spring bean and then construct a DatabasePoolTestUtils instance in the `@BeforeEach` method, like this:

```java
    @Autowire
    private DbaaSPostgresDbCreationService creationService;

    private DatabasePoolTestUtils databasePoolTestUtils;

    @BeforeEach
    public void init(){
        databasePoolTestUtils = new DatabasePoolTestUtils(creationService);
    }
```
#### Clear database pool cache
`DatabasePoolTestUtils` provides a `clearCache()` method that clears the cache and removes all previously registered classifiers
and database clients from the cache. To use this method, you should [initialize](#initialization) the `DatabasePoolTestUtils` and call `clearCache().`
It's a good practice to call this method in the `@AfterEach` method of your JUnit tests.

```java
    @AfterEach
    public void tearDown(){
        databasePoolTestUtils.clearCache();
    }
```
