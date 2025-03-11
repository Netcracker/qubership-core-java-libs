package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.MicroserviceClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.classifier.TenantClassifierBuilder;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.service.PersonService;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class TestUtils {

    public static final Map<String, Object> BASE_CLASSIFIER = Map.of("microserviceName", "testcontainers-microservice");

    public static final MicroserviceClassifierBuilder MICROSERVICE_CLASSIFIER_BUILDER = new MicroserviceClassifierBuilder(new HashMap<>(BASE_CLASSIFIER));
    public static final TenantClassifierBuilder TENANT_CLASSIFIER_BUILDER = new TenantClassifierBuilder(new HashMap<>(BASE_CLASSIFIER));

    public static final String FIRST_TENANT_ID = "testcontainers-first-tenant";
    public static final String SECOND_TENANT_ID = "testcontainers-second-tenant";

    public static void cleanupTenantDatabases(ContainerLogicalDbProvider logicalDbProvider, PersonService personService) {
        log.info("Perform cleanup for all created tenant databases in container");
        logicalDbProvider.forEachCreatedDatabase((clf, db) -> {
            Object tenantId = clf.asMap().get("tenantId");
            if (tenantId instanceof String) {
                String tenantIdStr = (String) tenantId;
                TenantContext.set(tenantIdStr);
                personService.deleteAll();
                log.info("Tenant data for tenant={} removed successfully", tenantIdStr);
                TenantContext.clear();
            }
        });
    }
}
