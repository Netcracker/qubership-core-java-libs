# Cassandra Client Migration

This library supports migration for Cassandra databases.


## Usage

1. Add maven dependency

    ```xml
    <dependency>
        <groupId>com.netcracker.cloud</groupId>
        <artifactId>dbaas-client-cassandra-migration</artifactId>
        <version>last_version</version>
    </dependency>
    ```

2. Create MigrationExecutor instance with builder if you use the default implementation:

```java
MigrationExecutor migrationExecutor = MigrationExecutorImpl.builder().build();
```
This will create migrationExecutor with default configuration. You can customize MigrationExecutor by providing builder with following:

- SchemaMigrationSettings - stores various configuration parameters for migration;
```java
MigrationExecutor migrationExecutor = MigrationExecutorImpl.builder().withSchemaMigrationSettingsBuilder(customSchemaMigrationSettingsBuilder).build();
```

- SchemaVersionResourceReader - provides the ability to read resources holding schema versions. 
Default implementation loads .cql and .cql.ftl resource types from pathes specified in VersionSettings of SchemaMigrationSettings.
```java
MigrationExecutor migrationExecutor = MigrationExecutorImpl.builder().withSchemaVersionResourceReader(customSchemaVersionResourceReader).build();
```

- AlreadyMigratedVersionsExtensionPoint - Provides information about schema versions that were already migrated before library usage. Optional extension point that is null by default. The library provides only the interface that can be implemented, see [here](src/main/java/org/qubership/cloud/dbaas/client/cassandra/migration/service/extension/AlreadyMigratedVersionsExtensionPoint.java).
```java
MigrationExecutor migrationExecutor = MigrationExecutorImpl.builder().withAlreadyMigratedVersionsExtensionPoint(customAlreadyMigratedVersionsExtensionPoint).build();
```


3. Perform migration by calling migrate(CqlSession session) method of the MigrationExecutor

```java
migrationExecutor.migrate(cqlSession);
```

## Schema Version script templating

FreeMarker is supported as templating engine.
Templating is applied only for scripts loaded from `.cql.ftl` resources.\
Resource with additional definitions may be provided using `TemplateSettings#templateDefinitionsResourcePath`.
These definitions will be available in templates under `fn` namespace, e.g. `<@fn.some_macro/>`.
`IS_AMAZON_KEYSPACES` variable holding the value of `AmazonKeyspacesSettings#enabled` is available in all
templates.
