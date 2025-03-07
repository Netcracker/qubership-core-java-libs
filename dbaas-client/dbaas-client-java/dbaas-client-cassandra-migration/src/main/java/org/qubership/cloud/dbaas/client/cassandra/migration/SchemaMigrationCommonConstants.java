package org.qubership.cloud.dbaas.client.cassandra.migration;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;

import org.apache.commons.lang3.SystemUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class SchemaMigrationCommonConstants {
    public static final String DEFAULT_RESOURCE_SEPARATOR = "/";
    public static final String DEFAULT_SCHEMA_MIGRATION_LOCK_TABLE_NAME = "schema_migration_lock";
    public static final String DEFAULT_SCHEMA_HISTORY_TABLE_NAME = "flyway_schema_history";
    public static final String MIGRATION_LOG_PREFIX = "[SCHEMA MIGRATION] - ";
    public static final String DB_MIGRATION_PATH = "db" + DEFAULT_RESOURCE_SEPARATOR + "migration" + DEFAULT_RESOURCE_SEPARATOR + "cassandra";
    public static final String DEFAULT_DEFINITIONS_RESOURCE_PATH = DB_MIGRATION_PATH + DEFAULT_RESOURCE_SEPARATOR + "templating" + DEFAULT_RESOURCE_SEPARATOR + "definitions.ftl";
    public static final String DEFAULT_SETTINGS_RESOURCE_PATH = DB_MIGRATION_PATH + DEFAULT_RESOURCE_SEPARATOR + "settings.json";
    public static final String DEFAULT_VERSIONS_DIRECTORY_PATH = DB_MIGRATION_PATH + DEFAULT_RESOURCE_SEPARATOR + "versions";
    public static final String DEFAULT_RESOURCE_NAME_PATTERN = "V(.+)__(.+)\\.(.+)";

    public static final String HOST_NAME = SystemUtils.getHostName();
}
