package org.qubership.cloud.core.quarkus.dbaas.datasource.config.properties;

import io.quarkus.runtime.annotations.ConfigGroup;
import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;

import java.util.HashMap;
import java.util.Map;

@ConfigRoot(name = "dbaas", phase = ConfigPhase.RUN_TIME)
public class DatasourceProperties {

    /**
     * jdbc config
     */
    @ConfigItem(name = "datasource.jdbc")
    public JDBCConfig jdbc;

    /**
     * enhanced-leak-report
     */
    @ConfigItem(name = "datasource.enhanced-leak-report.enable", defaultValue = "false")
    public Boolean enhancedLeakReport;

    /**
     * debug-listener
     */
    @ConfigItem(name = "datasource.debug-listener.enable", defaultValue = "false")
    public Boolean debugDatasourceListeners;

    /**
     * globalJdbcProperties
     */
    @ConfigItem(name = "datasource.jdbc-properties")
    public Map<String, String> globalJdbcProperties = new HashMap<>();

    /**
     * global xaProperties
     */
    @ConfigItem(name = "datasource.xa-properties")
    public Map<String, String> globalXaProperties = new HashMap<>();

    /**
     * global XA configuration
     */
    @ConfigItem(name = "datasource.xa", defaultValue = "false")
    public Boolean xa;

    /**
     * jdbc
     */
    @ConfigMapping
    public Map<String, JDBCProperties> datasources = new HashMap<>();

    @ConfigGroup
    public static class JDBCProperties {

        /**
         * jdbc config
         */
        @ConfigItem(name = "jdbc")
        public JDBCConfig jdbc;

        /**
         * jdbcProperties
         */
        @ConfigItem(name = "jdbc-properties")
        public Map<String, String> jdbcProperties = new HashMap<>();

        /**
         * xaProperties
         */
        @ConfigItem(name = "xa-properties")
        public Map<String, String> xaProperties = new HashMap<>();

        /**
         * is XA datasource
         */
        @ConfigItem(name = "xa", defaultValue = "false")
        public Boolean xa;
    }

}
