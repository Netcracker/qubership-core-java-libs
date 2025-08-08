package com.netcracker.cloud.dbaas.client.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

class DbaasPostgresDataSourcPropertiesTest {

    @Test
    void setDataSource() {
        DbaasPostgresDataSourceProperties dp = new DbaasPostgresDataSourceProperties();
        Map<Object, Object> datasource = new HashMap<Object, Object>();
        datasource.put("connection-properties", "options=-c test");
        dp.setDatasource(datasource);
        Assertions.assertEquals("options=-c test -c idle-in-transaction-session-timeout=28800000", datasource.get("connection-properties"));
    }

    @Test
    void setDataSourceWhenOptionMissing() {
        DbaasPostgresDataSourceProperties dp = new DbaasPostgresDataSourceProperties();
        Map<Object, Object> datasource = new HashMap<Object, Object>();
        datasource.put("connection-properties", "");
        dp.setDatasource(datasource);
        String connectionProperties = datasource.get("connection-properties").toString();
        Assertions.assertTrue(connectionProperties.contains("option"));
    }
}
