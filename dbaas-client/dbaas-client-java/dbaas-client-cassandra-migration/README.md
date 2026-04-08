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
  Default implementation loads .cql and .cql.ftl resource types from path's specified in VersionSettings of
  SchemaMigrationSettings.

```java
MigrationExecutor migrationExecutor = MigrationExecutorImpl.builder().withSchemaVersionResourceReader(customSchemaVersionResourceReader).build();
```

- AlreadyMigratedVersionsExtensionPoint - provides information about schema versions that were already migrated before library usage.
  Optional extension point that is null by default. The library provides only the interface that can be implemented,
  see [here](../dbaas-client-cassandra-migration/src/main/java/com/netcracker/cloud/dbaas/client/cassandra/migration/service/extension/AlreadyMigratedVersionsExtensionPoint.java).
  This interface accepts CqlSession instance, so you can check what was already applied to the DB to skip some of your migrations.
  This may be useful in case of adopting this library to the service where some different migration approach was used before.

```java
MigrationExecutor migrationExecutor = MigrationExecutorImpl.builder().withAlreadyMigratedVersionsExtensionPoint(customAlreadyMigratedVersionsExtensionPoint).build();
```

3. Perform migration by calling migrate(CqlSession session) method of the MigrationExecutor

```java
migrationExecutor.migrate(cqlSession);
```

It is possible to override migration implementation by registering your own bean implementing
`com.netcracker.cloud.dbaas.client.cassandra.migration.MigrationExecutor` interface.
In that case default migration logic provided by the library will be ignored.

## Overview

The library will load migration scripts from the `db/migration/cassandra/versions/` path and execute them.
Migration scripts need to follow the following naming pattern: `V{version}__{description}.{format}`. For example:
`V1__first_migration.cql`, `V2__template_migration.cql.ftl`, `V2.1_01__migration_patch.cql`.
Order of the migration scripts execution is based on the {version} number of script.

You can disable this migration implementation by setting `dbaas.cassandra.migration.enabled` property to `false` in your
application configuration file.

All information about the applied migrations will be stored in a `flyway_schema_history` table. This name can be changed
by a property (next section) or by providing a custom SchemaMigrationSettings instance to MigrationExecutor.

### Configuration

Due to the Cassandra specific lock is acquired for the limited time period and then periodically extended.

| name                                                                      | default                                 | description                                                                                                                                                                     |
|---------------------------------------------------------------------------|-----------------------------------------|---------------------------------------------------------------------------------------------------------------------------------------------------------------------------------|
| dbaas.cassandra.migration.schema-history-table-name                       | flyway_schema_history                   | name of the table to store schema version history                                                                                                                               |
| dbaas.cassandra.migration.version.settings-resource-path                  | db/migration/settings.json              | resource path to get additional schema version settings. See also `SchemaVersionSettings` javadoc                                                                               |
| dbaas.cassandra.migration.version.directory-path                          | db/migration/versions                   | directory path to scan for schema version resources                                                                                                                             |
| dbaas.cassandra.migration.version.resource-name-pattern                   | "V(.+)__(.+)\\.(.+)"                    | pattern to get information about schema version from resource name, must contain ordered groups for 1 - version, 2 - description, 3 - resource type                             |
| dbaas.cassandra.migration.template.definitions-resource-path              | db/migration/templating/definitions.ftl | resource path to get additional definitions to import into FreeMarker configuration and allow to be used in schema version scripts under fn namespace                           |
| dbaas.cassandra.migration.lock.table-name                                 | schema_migration_lock                   | name of the table for migration locks holding                                                                                                                                   |
| dbaas.cassandra.migration.lock.retry-delay                                | 5 000 (mills)                           | delay between attempts to acquire the lock                                                                                                                                      |
| dbaas.cassandra.migration.lock.lock-lifetime                              | 60 000 (mills)                          | lock lifetime                                                                                                                                                                   |
| dbaas.cassandra.migration.lock.extension-period                           | 5 000 (mills)                           | lock extension period                                                                                                                                                           |
| dbaas.cassandra.migration.lock.extension-fail-retry-delay                 | 500 (mills)                             | lock extension delay after the extension failure. Will be applied until the extension success or lock-lifetime is passed.                                                       |
| dbaas.cassandra.migration.schema-agreement.await-retry-delay              | 500 (mills)                             | retry delay for schema agreement await                                                                                                                                          |
| dbaas.cassandra.migration.amazon-keyspaces.enabled                        | false                                   | true if Amazon Keyspaces is used instead of Cassandra                                                                                                                           |
| dbaas.cassandra.migration.amazon-keyspaces.table-status-check.pre-delay   | 1 000 (mills)                           | preliminary delay before checking table status in system_schema_mcs.tables. Is required because Amazon Keyspaces updates the status in system_schema_mcs.tables asynchronously. |
| dbaas.cassandra.migration.amazon-keyspaces.table-status-check.retry-delay | 500 (mills)                             | retry delay for checking expected table statuses in system_schema_mcs.tables                                                                                                    |

### Migration settings

All migrations should be registered in special `settings.json` file at the `db/migration/cassandra/` path.
The script version numbers should be mapped to their specific configuration in the following format:

```json
{
  "{version1}": {
    "someSetting": ...
  },
  "{version2}": {
    "someSetting": ...
  },
  "{version3}": {
  }
}
```

Even if some migration does not require special settings, it should be registered in this file - just make an empty section for it.

#### Ignore certain errors

It is possible to configure to ignore certain errors during migration scripts execution based on database response messages.
Such configuration can be provided in the following format:

```json
{
  "{version}": {
    "ignoreErrorPatterns": [
      "message_regex"
    ]
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

1. Set `dbaas.cassandra.migration.amazonKeyspaces` property to `true` in your application configuration file;
2. Provide additional configuration in `settings.json` that describes DDL (UPDATE, CREATE, DROP) operations of the
   migration scripts in the following format:

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

where `TABLE_NAME` - is the name of the table that is being migrated and `OPERATION_TYPE` is DDL operation type (one of
the following: UPDATE, CREATE, DROP).

If for example your `V1.0__schema_migration.cql` script contains creation of `sample_migration_table_1` and changes to
`sample_migration_table_2` table then `settings.json` should have following:

```json
{
  "1.0": {
    "tableOperations": [
      {
        "tableName": "sample_migration_table_1",
        "operationType": "CREATE"
      },
      {
        "tableName": "sample_migration_table_2",
        "operationType": "UPDATE"
      }
    ]
  }
}
```

These operation types will be used to correctly wait for migrations to complete in Amazon Keyspaces.

#### Changes in existing migrations

Sometimes you need to make changes to existing migrations that could have already been applied in some databases.
If you simply change the migration code, it will run with an error about a mismatch of the checksums - because the
checksum of the modified migration will differ from the checksum of the old one, which was already applied to the database.

If you need to make changes to an existing migration, you can add the following section to `settings.json` file:

```json
{
  "2.0": {
    "previousStates": [
      {
        "checksum": "111",
        "invalid": true,
        "changeReason": "Wrong table name"
      },
      {
        "checksum": "222",
        "invalid": false,
        "changeReason": "Apply formatting"
      }
    ]
  }
}
```

Let's assume that you've already made two changes to migration with version `2.0`, and now it has a `333` checksum. In this example
we have two previous states for this migration described in `settings.json` file. If during the migration process it turns out
that this migration version with a `111` checksum has already been applied to the database, then its new version (`333` checksum) will be applied on top,
since this previous state is marked as `invalid`. If during the migration process it turns out that this migration version with a `222` checksum
has already been applied to the database, then migration `2.0` will just be skipped, since this previous state is not marked as `invalid`.

You can get the old state checksum from the error message when applying a new migration without `previousStates` setting.

## Templates usage

Migration feature also provides integration with Apache FreeMarker template engine, and you can use templates written in
the FreeMarker Template Language as your migration scripts.
They will be processed by the template engine and the result will be executed as a regular cql script.
Templating is applied only for scripts loaded from `.cql.ftl` resources.

Additionally, you can have your own template definitions specified by the `dbaas.cassandra.migration.template.definitions-resource-path`
property or by using `TemplateSettings#templateDefinitionsResourcePath` - they will be automatically [imported](https://freemarker.apache.org/docs/ref_directive_import.html)
with `fn` hash, e.g. `<@fn.some_macro/>`.
It can be used for the convenient storage of macros for your templates.

`IS_AMAZON_KEYSPACES` variable holding the value of `AmazonKeyspacesSettings#enabled` is available in all templates.
