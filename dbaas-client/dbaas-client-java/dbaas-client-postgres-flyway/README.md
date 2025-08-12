# DBaaS Client Flyway

This library supports configuring flyway migrations for multiple postgres datasource.

## Usage

1. Add maven dependency

    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-postgres-flyway</artifactId>
        <version>last_version</version>
    </dependency>
    ```
2. Add `@EnableFlywayPostgresql` annotation to one of your application configuration classes. 
3. Flyway migration scripts will be automatically executed after database creation.
   * For shared database by default migration scripts have to be placed into `classpath:db/migration/postgresql/static` .
   * For named versioned database you have to place migration scripts into `classpath:db/migration/postgresql/{logicalDbName_value}.`
     By default database with 'logicalDbName' within classifier is a versioned database for flyway script.

> **Pay attention**! This functionality is built upon the PostConnectProcessor. The order of this PostConnectProcessor
> is set to 100. Therefore, all your PostConnectProcessors must have an order greater than 100 if you want them to be
> executed after the Flyway migration.

`logicalDbName_value` here is an additional field in classifier which is used in order to distinguish between databases. 
We recommend to add "logicalDbName" for versioned databases. More instructions how to add such field may be found in 
[Datasource for versioned database classifier](./dbaas-client-java/dbaas-client-postgres-starter/README.md#datasource-for-versioned-database-classifier)


## Configuration

| Property Name                                                | Description                                                                                                                                                                                                                               | Default                                            |
|--------------------------------------------------------------|-------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|----------------------------------------------------|
| dbaas.postgres.datasource.flyway.locations                   | Path to migration scripts for static (non-versioned, shared) database                                                                                                                                                                     | classpath:db/migration/postgresql/static           |
| dbaas.postgres.datasources.{logicalDbName}.flyway.locations. | Path to migration scripts for versioned database                                                                                                                                                                                          | classpath:db/migration/postgresql/{logicalDbName}. |
| dbaas.postgres.datasource.flyway.*                           | All [classic flyway properties](https://documentation.red-gate.com/fd/parameters-184127474.html) for default datasource should be passed with such prefix, eg. dbaas.postgres.datasource.flyway.baselineOnMigrate=true                    | -                                                  |
| dbaas.postgres.datasources.{logicalDbName}.flyway.*          | All [classic flyway properties](https://documentation.red-gate.com/fd/parameters-184127474.html) for versioned datasource should be passed with such prefix, eg. dbaas.postgres.datasources.{logicalDbName}.flyway.baselineOnMigrate=true | -                                                  |

In case of property with multiple values pass them comma separated (see example with 'ignoreMigrationPatterns' below).

Example of configuration
```yaml
dbaas:
  postgres:
    datasource:
      flyway:
        baselineOnMigrate: true
        ignoreMigrationPatterns: "*:future, *:missing"
    datasources:
      configs:
        flyway:
          validate-on-migrate: true
          executeInTransaction: false
          locations: classpath:db/migration/postgresql/configs/1, classpath:db/migration/postgresql/configs/2
```

## Custom flyway configuration

Also, it is possible to define user's custom flyway configuration. 
To do it just define own `@Bean("flywayPostgresPostProcessor")` which will return class like in bellow example

```java
public class FlywayPostgresPostProcessor implements PostConnectProcessor<PostgresDatabase> {     
 
    @Value("dbaas.api.postgresql.flyway.baseline-on-migrate:false")
    boolean baselineMigrate;
 
    @Value("validate-on-migrate:true")
    boolean validateOnMigrate;
 
    @Override
    public void process(PostgresDatabase postgresDatabase) {
        String databaseName = postgresDatabase.getName();
        log.info("Starting Flyway migration for database: {}", databaseName);
        FluentConfiguration configure = Flyway.configure()
                .dataSource(postgresDatabase.getConnectionProperties().getDataSource())
                .baselineOnMigrate(baselineMigrate)
                .validateOnMigrate(validateOnMigrate);
        configure = configure.locations("classpath:db/migration");
         
        configure.load().migrate();
        log.info("Finished Flyway migration for database: {}", databaseName);
    }
}
```

## Example

Below example is based on default configuration.

User has two databases: general db with business data (not versioned) and configuration database (which is versioned)

Business db has classifier
```json
"classifier" : {
    "scope": "service",
    "microserviceName": "test-name",
    "namespace": "test-namespace"
}
```
And flyway scripts for this database should be placed into `classpath:db/migration/postgresql/static`

Configuration database has classifier
```json
"classifier" : {
    "scope": "service",
    "microserviceName": "test-name",
    "namespace": "test-namespace",
    "customKeys":{
        "logicalDbName": "configs"
    }
}
```
And flyway scripts for this database should be placed into `classpath:db/migration/postgresql/configs` because in classifier "logicalDbName"="configs"

Properties for flyway configuration
```yaml
dbaas:
  postgres:
    datasource:
      flyway:
        baselineOnMigrate: true
    datasources:
      configs:
        flyway:
          validate-on-migrate: true
```
