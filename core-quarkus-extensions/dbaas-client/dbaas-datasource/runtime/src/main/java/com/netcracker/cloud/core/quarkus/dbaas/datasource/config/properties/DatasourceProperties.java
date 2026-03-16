package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;
import io.smallrye.config.WithParentName;

import java.util.Map;

@ConfigMapping(prefix = "quarkus.dbaas")
@ConfigRoot(phase = ConfigPhase.RUN_TIME)
public interface DatasourceProperties {

    /**
     * jdbc config
     */
    @WithName("datasource.jdbc")
    JDBCConfig jdbc();

    /**
     * enhanced-leak-report
     */
    @WithName("datasource.enhanced-leak-report.enable")
    @WithDefault("false")
    boolean enhancedLeakReport();

    /**
     * debug-listener
     */
    @WithName("datasource.debug-listener.enable")
    @WithDefault("false")
    boolean debugDatasourceListeners();

    /**
     * globalJdbcProperties
     */
    @WithName("datasource.jdbc-properties")
    Map<String, String> globalJdbcProperties();

    /**
     * global xaProperties
     */
    @WithName("datasource.xa-properties")
    Map<String, String> globalXaProperties();

    /**
     * global XA configuration
     */
    @WithName("datasource.xa")
    @WithDefault("false")
    boolean xa();

    /**
     * jdbc
     */
    @WithParentName
    Map<String, JDBCProperties> datasources();

    interface JDBCProperties {

        /**
         * jdbc config
         */
        @WithName("jdbc")
        JDBCConfig jdbc();

        /**
         * jdbcProperties
         */
        @WithName("jdbc-properties")
        Map<String, String> jdbcProperties();

        /**
         * xaProperties
         */
        @WithName("xa-properties")
        Map<String, String> xaProperties();

        /**
         * is XA datasource
         */
        @WithName("xa")
        @WithDefault("false")
        boolean xa();
    }
}
