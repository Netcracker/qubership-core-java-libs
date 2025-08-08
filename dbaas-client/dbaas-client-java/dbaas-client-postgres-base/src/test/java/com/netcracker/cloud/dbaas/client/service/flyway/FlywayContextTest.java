package org.qubership.cloud.dbaas.client.service.flyway;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import javax.sql.DataSource;

import static org.junit.jupiter.api.Assertions.assertNotNull;

class FlywayContextTest {

    @Test
    void getDataSource() {
        DataSource dataSource = Mockito.mock(DataSource.class);
        FlywayContext flywayContext = new FlywayContext(dataSource);
        assertNotNull(flywayContext.getDataSource());
    }
}