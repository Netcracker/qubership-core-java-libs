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
public class DatasourcePropertiesTest {

    @Inject
    DatasourceProperties datasourceProperties;

    @Test
    public void globalInitialSqlIsRead() {
        Assertions.assertEquals(Profile.GLOBAL_VALUE, datasourceProperties.initialSql().orElseThrow());
    }

    @Test
    public void perDatasourceInitialSqlIsRead() {
        Assertions.assertEquals(
                Profile.PER_DB_VALUE,
                datasourceProperties.datasources().get(Profile.LOGICAL_DB).initialSql().orElseThrow()
        );
    }

    @NoArgsConstructor
    public static final class Profile implements QuarkusTestProfile {
        static final String GLOBAL_VALUE = "SET TIME ZONE 'UTC'";
        static final String LOGICAL_DB = "configs";
        static final String PER_DB_VALUE = "SET search_path TO my_schema";

        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> props = new HashMap<>();
            props.put("quarkus.dbaas.datasource.initial-sql", GLOBAL_VALUE);
            props.put("quarkus.dbaas.datasources." + LOGICAL_DB + ".initial-sql", PER_DB_VALUE);
            return props;
        }

        @Override
        public boolean disableGlobalTestResources() {
            return true;
        }
    }
}
