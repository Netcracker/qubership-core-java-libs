package org.qubership.cloud.quarkus.dbaas.opensearch.client.config;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;

import java.util.Map;
import java.util.Optional;

@ConfigRoot(name = "dbaas.opensearch.api", phase = ConfigPhase.RUN_TIME)
public class DbaaSOpensearchCreationConfig {
    /**
     * Property with opensearch creation parameters for service database.
     */
    @ConfigItem(name = "service")
    public OpensearchConfiguration serviceDbConfiguration;

    /**
     * Property with opensearch creation parameters for tenant databases.
     */
    @ConfigItem(name = "tenant")
    public Map<String, OpensearchConfiguration> tenantDbConfiguration;

    /**
     * Property with opensearch creation parameters for all tenant database.
     */
    @ConfigItem(name = "tenant")
    public OpensearchConfiguration singleTeantDbConfig;


    /**
     * Property with user role for outgoing requests.
     */
    @ConfigItem
    public Optional<String> runtimeUserRole;


    public OpensearchConfiguration getOpensearchConfiguration(String tenantId) {
        if (tenantId != null) {
            OpensearchConfiguration opensearchConfiguration = tenantDbConfiguration.get(tenantId);
            return opensearchConfiguration != null ? opensearchConfiguration : tenantDbConfiguration.get("tenant");
        } else {
            return serviceDbConfiguration;
        }
    }
}
