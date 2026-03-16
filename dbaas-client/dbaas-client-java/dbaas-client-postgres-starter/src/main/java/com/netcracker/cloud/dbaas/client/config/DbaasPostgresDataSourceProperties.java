package com.netcracker.cloud.dbaas.client.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

@ConfigurationProperties(prefix = "dbaas.postgres")
public class DbaasPostgresDataSourceProperties {
    private static final String OPTIONS = "options=";
    private static final String CONNECTION_PROP = "connection-properties";
    private Class<?> dataSourceType;
    private Map<Object, Object> datasource;

    public DbaasPostgresDataSourceProperties() {
        datasource = new LinkedHashMap<>();
        datasource.put(CONNECTION_PROP, "options=-c idle-in-transaction-session-timeout=28800000");
    }

    public Map<Object, Object> getDatasource() {
        return datasource;
    }

    public void setDatasource(Map<Object, Object> datasource) {
        String connectionProperties = datasource.get(CONNECTION_PROP).toString();
        if (!connectionProperties.contains(OPTIONS)) {
            datasource.put(CONNECTION_PROP, connectionProperties + ";options=-c idle-in-transaction-session-timeout=28800000");
        } else {
            String[] properties = connectionProperties.split(";");
            Optional<String> optionsValue = Arrays.stream(properties).filter(o -> o.contains(OPTIONS)).findFirst();
            if (optionsValue.isPresent() && !optionsValue.get().contains("idle-in-transaction-session-timeout")) {
                String finalVal = optionsValue.get().concat(" -c idle-in-transaction-session-timeout=28800000");
                for (int i = 0; i < properties.length; i++) {
                    if (properties[i].contains(OPTIONS)) {
                        properties[i] = finalVal;
                    }
                }
                connectionProperties = String.join(";", properties);
                datasource.put(CONNECTION_PROP, connectionProperties);
            }
        }
        datasource.remove("flyway");
        this.datasource = datasource;
    }

    public Class<?> getDataSourceType() {
        return dataSourceType;
    }

    public void setDataSourceType(Class<?> dataSourceType) {
        this.dataSourceType = dataSourceType;
    }
}
