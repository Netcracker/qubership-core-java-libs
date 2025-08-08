package com.netcracker.cloud.dbaas.client.opensearch.entity;

import com.netcracker.cloud.dbaas.client.opensearch.entity.metrics.OpensearchClientRequestsSecondsMetricsProperties;
import lombok.Data;

@Data
public class DbaasOpensearchMetricsProperties {

    private Boolean enabled = Boolean.TRUE;
    private OpensearchClientRequestsSecondsMetricsProperties requestsSeconds = new OpensearchClientRequestsSecondsMetricsProperties();
}
