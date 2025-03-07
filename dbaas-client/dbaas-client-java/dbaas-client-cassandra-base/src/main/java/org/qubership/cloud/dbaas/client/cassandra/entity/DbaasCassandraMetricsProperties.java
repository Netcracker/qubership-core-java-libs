package org.qubership.cloud.dbaas.client.cassandra.entity;

import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.NodeMetricsConfiguration;
import org.qubership.cloud.dbaas.client.cassandra.entity.metrics.SessionMetricsConfiguration;
import lombok.Data;

@Data
public class DbaasCassandraMetricsProperties {

    private Boolean enabled = Boolean.TRUE;
    private SessionMetricsConfiguration session = new SessionMetricsConfiguration();
    private NodeMetricsConfiguration node = new NodeMetricsConfiguration();

}
