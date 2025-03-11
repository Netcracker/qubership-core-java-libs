package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import org.qubership.cloud.dbaas.client.DbaasClient;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.context.Dependent;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import lombok.extern.slf4j.Slf4j;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource.postgresql;

@Slf4j
@Dependent
public class PostgresqlTestContainersConfiguration {

    @Produces
    @Priority(1)
    @Alternative
    @ApplicationScoped
    public ContainerLogicalDbProvider containerLogicalDbProvider() {
        return new ContainerLogicalDbProvider(postgresql);
    }

    @Produces
    @Priority(1)
    @Alternative
    public DbaasClient dbaaSClientCommon() {
        return null;
    }
}
