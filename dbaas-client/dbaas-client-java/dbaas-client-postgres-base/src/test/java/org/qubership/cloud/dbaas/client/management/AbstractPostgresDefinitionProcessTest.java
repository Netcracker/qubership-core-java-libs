package org.qubership.cloud.dbaas.client.management;

import org.qubership.cloud.dbaas.client.entity.database.PostgresDatabase;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

public class AbstractPostgresDefinitionProcessTest {
    @Test
    public void getSupportedDatabaseType() {
        AbstractPostgresDefinitionProcess abstractPostgresDefinitionProcess = new AbstractPostgresDefinitionProcess() {
            @Override
            public void process(PostgresDatabase database) {

            }
        };
        Assertions.assertEquals(PostgresDatabase.class, abstractPostgresDefinitionProcess.getSupportedDatabaseType());
    }
}