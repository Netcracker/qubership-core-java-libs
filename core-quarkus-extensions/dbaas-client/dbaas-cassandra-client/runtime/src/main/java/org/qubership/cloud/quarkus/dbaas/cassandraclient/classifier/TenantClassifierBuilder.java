package org.qubership.cloud.quarkus.dbaas.cassandraclient.classifier;

import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import org.qubership.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;

import java.util.Map;

import static org.qubership.cloud.dbaas.client.DbaasConst.SCOPE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

public class TenantClassifierBuilder implements DbaaSClassifierBuilder {
    private final Map<String, Object> primaryClassifier;

    public TenantClassifierBuilder(Map<String, Object> primaryClassifier) {
        this.primaryClassifier = primaryClassifier;
    }

    @Override
    public DbaasDbClassifier build() {
        primaryClassifier.put(SCOPE, TENANT);
        primaryClassifier.put("tenantId", ((TenantContextObject) ContextManager.get(TENANT_CONTEXT_NAME)).getTenant());
        return new DbaasDbClassifier(primaryClassifier);
    }
}
