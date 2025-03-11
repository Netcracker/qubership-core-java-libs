package org.qubership.cloud.core.quarkus.dbaas.testutils;

import org.qubership.cloud.core.quarkus.dbaas.datasource.service.DbaaSPostgresDbCreationService;
import org.qubership.cloud.core.quarkus.dbaas.datasource.service.impl.DbaaSPostgresDbCreationServiceImpl;

import java.lang.reflect.Field;
import java.util.Map;

public class DatabasePoolTestUtils {
    private DbaaSPostgresDbCreationService dataSourceCreationService;

    public DatabasePoolTestUtils(DbaaSPostgresDbCreationService dataSourceCreationService) {
        this.dataSourceCreationService = dataSourceCreationService;
    }

    public void clearCache() {
        try {
            clearMap("postgresDbMap");
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException("unable to clean database pool cache", ex);
        }
    }

    private void clearMap(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = DbaaSPostgresDbCreationServiceImpl.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(this.dataSourceCreationService)).clear();
    }
}
