package org.qubership.cloud.junit.cloudcore.extension.provider;

import lombok.Data;

@Data
public class PortForwardConfig {
    private String cloud;
    private String namespace;

    public PortForwardConfig(String cloud, String namespace) {
        this.cloud = cloud;
        this.namespace = namespace;
    }
}
