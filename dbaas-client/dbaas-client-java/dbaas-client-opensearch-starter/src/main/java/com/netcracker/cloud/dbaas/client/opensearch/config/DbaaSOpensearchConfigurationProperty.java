package com.netcracker.cloud.dbaas.client.opensearch.config;

import com.netcracker.cloud.dbaas.client.opensearch.entity.DbaasOpensearchMetricsProperties;
import lombok.Data;

@Data
public class DbaaSOpensearchConfigurationProperty {
    private Integer maxConnTotal;
    private Integer maxConnPerRoute;

    private DbaasOpensearchMetricsProperties metrics = new DbaasOpensearchMetricsProperties();
}
