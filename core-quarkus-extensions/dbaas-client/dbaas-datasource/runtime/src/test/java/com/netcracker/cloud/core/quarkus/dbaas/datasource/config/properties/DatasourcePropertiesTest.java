package com.netcracker.cloud.core.quarkus.dbaas.datasource.config.properties;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;


@QuarkusTest
@TestProfile(DatasourcePropertiesTest.Profile.class)
class DatasourcePropertiesTest {

    @Inject
    DatasourceProperties datasourceProperties;

    @Test
    void globalInitialSqlIsRead() {
        Assertions.assertEquals(Profile.GLOBAL_VALUE, datasourceProperties.initialSql().orElseThrow());
    }

    @Test
    void perDatasourceInitialSqlIsRead() {
        Assertions.assertEquals(
                Profile.PER_DB_VALUE,
                datasourceProperties.datasources().get(Profile.LOGICAL_DB).initialSql().orElseThrow()
        );
    }

    @Test
    void enhancedLeakReportIsRead() {
        Assertions.assertTrue(datasourceProperties.enhancedLeakReport());
    }

    @Test
    void debugDatasourceListenersIsRead() {
        Assertions.assertTrue(datasourceProperties.debugDatasourceListeners());
    }

    @Test
    void globalXaIsRead() {
        Assertions.assertTrue(datasourceProperties.xa());
    }

    @Test
    void globalJdbcPropertiesIsRead() {
        Assertions.assertEquals(Profile.JDBC_PROP_VALUE, datasourceProperties.globalJdbcProperties().get(Profile.JDBC_PROP_KEY));
    }

    @Test
    void globalXaPropertiesIsRead() {
        Assertions.assertEquals(Profile.XA_PROP_VALUE, datasourceProperties.globalXaProperties().get(Profile.XA_PROP_KEY));
    }

    @Test
    void globalJdbcConfigIsRead() {
        JDBCConfig jdbc = datasourceProperties.jdbc();
        Assertions.assertEquals(10, jdbc.poolSize());
        Assertions.assertEquals(2, jdbc.minPoolSize());
        Assertions.assertEquals(3, jdbc.initPoolSize());
        Assertions.assertEquals(60.0, jdbc.datasourceValidationInterval());
        Assertions.assertEquals(1.5, jdbc.datasourceIdleValidationTimeout());
        Assertions.assertEquals(2.5, jdbc.datasourceReapTimeout());
        Assertions.assertEquals(45.0, jdbc.datasourceAcquisitionTimeout());
        Assertions.assertEquals("10", jdbc.datasourceRespondTimeToDrop());
        Assertions.assertEquals(15.0, jdbc.datasourceLeakDetectionInterval());
        Assertions.assertFalse(jdbc.autoCommit());
        Assertions.assertTrue(jdbc.flushOnClose());
    }

    @Test
    void perDatasourceJdbcConfigIsRead() {
        JDBCConfig jdbc = datasourceProperties.datasources().get(Profile.LOGICAL_DB).jdbc();
        Assertions.assertEquals(20, jdbc.poolSize());
    }

    @Test
    void perDatasourceJdbcPropertiesIsRead() {
        Assertions.assertEquals(
                Profile.PER_JDBC_PROP_VALUE,
                datasourceProperties.datasources().get(Profile.LOGICAL_DB).jdbcProperties().get(Profile.JDBC_PROP_KEY)
        );
    }

    @Test
    void perDatasourceXaPropertiesIsRead() {
        Assertions.assertEquals(
                Profile.PER_XA_PROP_VALUE,
                datasourceProperties.datasources().get(Profile.LOGICAL_DB).xaProperties().get(Profile.XA_PROP_KEY)
        );
    }

    @Test
    void perDatasourceXaIsRead() {
        Assertions.assertTrue(datasourceProperties.datasources().get(Profile.LOGICAL_DB).xa());
    }

    @NoArgsConstructor
    public static final class Profile implements QuarkusTestProfile {
        static final String GLOBAL_VALUE = "SET TIME ZONE 'UTC'";
        static final String LOGICAL_DB = "configs";
        static final String PER_DB_VALUE = "SET search_path TO my_schema";
        static final String JDBC_PROP_KEY = "socketTimeout";
        static final String JDBC_PROP_VALUE = "30";
        static final String XA_PROP_KEY = "pinGlobalTxToPhysicalConnection";
        static final String XA_PROP_VALUE = "true";
        static final String PER_JDBC_PROP_VALUE = "60";
        static final String PER_XA_PROP_VALUE = "false";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> props = new HashMap<>();
            props.put("quarkus.dbaas.datasource.initial-sql", GLOBAL_VALUE);
            props.put("quarkus.dbaas.datasource.enhanced-leak-report.enable", "true");
            props.put("quarkus.dbaas.datasource.debug-listener.enable", "true");
            props.put("quarkus.dbaas.datasource.xa", "true");
            props.put("quarkus.dbaas.datasource.jdbc-properties." + JDBC_PROP_KEY, JDBC_PROP_VALUE);
            props.put("quarkus.dbaas.datasource.xa-properties." + XA_PROP_KEY, XA_PROP_VALUE);
            props.put("quarkus.dbaas.datasource.jdbc.max-size", "10");
            props.put("quarkus.dbaas.datasource.jdbc.min-size", "2");
            props.put("quarkus.dbaas.datasource.jdbc.initial-size", "3");
            props.put("quarkus.dbaas.datasource.jdbc.background-validation-interval.seconds", "60");
            props.put("quarkus.dbaas.datasource.jdbc.idle-removal-interval.seconds", "1.5");
            props.put("quarkus.dbaas.datasource.jdbc.idle-reap-interval.seconds", "2.5");
            props.put("quarkus.dbaas.datasource.jdbc.acquisition-timeout.seconds", "45");
            props.put("quarkus.dbaas.datasource.jdbc.respond-time-to-drop.seconds", "10");
            props.put("quarkus.dbaas.datasource.jdbc.leak-detection-interval.seconds", "15");
            props.put("quarkus.dbaas.datasource.jdbc.autocommit", "false");
            props.put("quarkus.dbaas.datasource.jdbc.flush-on-close", "true");
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".jdbc.max-size", "20");
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".jdbc-properties." + JDBC_PROP_KEY, PER_JDBC_PROP_VALUE);
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".xa-properties." + XA_PROP_KEY, PER_XA_PROP_VALUE);
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".xa", "true");
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".initial-sql", PER_DB_VALUE);
            return props;
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
