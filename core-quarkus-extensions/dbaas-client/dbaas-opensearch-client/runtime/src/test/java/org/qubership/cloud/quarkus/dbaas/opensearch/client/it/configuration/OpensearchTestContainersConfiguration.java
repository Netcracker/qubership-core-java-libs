package org.qubership.cloud.quarkus.dbaas.opensearch.client.it.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Dependent
public class OpensearchTestContainersConfiguration {

    @Produces
    @Priority(1)
    @Alternative
    @ApplicationScoped
    public ContainerLogicalDbProvider containerLogicalDbProvider() {
        return new ContainerLogicalDbProvider(OpensearchContainerResource.opensearchContainer);
    }

    @Produces
    @Priority(1)
    @Alternative
    public DbaasClient dbaaSClientCommon() {
        return null;
    }
}
