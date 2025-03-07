package org.qubership.cloud.dbaas.client.opensearch.config;

import org.qubership.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import lombok.Data;

@Data
public class DbaaSOpensearchConfigurationProperty {
    private Integer maxConnTotal;
    private Integer maxConnPerRoute;

    private DbaasOpensearchMetricsProperties metrics = new DbaasOpensearchMetricsProperties();
}