package com.netcracker.cloud.dbaas.client.test;

import com.netcracker.cloud.dbaas.client.management.DatabasePool;

import java.lang.reflect.Field;
import java.util.Map;

public class DatabasePoolTestUtils {

    private DatabasePool databasePool;

    public DatabasePoolTestUtils(DatabasePool databasePool) {
        this.databasePool = databasePool;
    }

    public void clearCache() {
        try {
            clearMap("databasesCacheL1");
            clearMap("databasesCacheL2");
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException("unable to clean database pool cache", ex);
        }
    }

    private void clearMap(String fieldName) throws NoSuchFieldException, IllegalAccessException {
        Field field = DatabasePool.class.getDeclaredField(fieldName);
        field.setAccessible(true);
        ((Map<?, ?>) field.get(this.databasePool)).clear();
    }
}
