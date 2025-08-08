package com.netcracker.cloud.dbaas.client.cassandra.entity;

import com.netcracker.cloud.dbaas.client.cassandra.entity.metrics.NodeMetricsConfiguration;
import com.netcracker.cloud.dbaas.client.cassandra.entity.metrics.SessionMetricsConfiguration;
import lombok.Data;

@Data
public class DbaasCassandraMetricsProperties {

    private Boolean enabled = Boolean.TRUE;
    private SessionMetricsConfiguration session = new SessionMetricsConfiguration();
    private NodeMetricsConfiguration node = new NodeMetricsConfiguration();

}
