package com.netcracker.cloud.quarkus.dbaas.mongoclient.classifier;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSClassifierBuilder;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;

import java.util.Map;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.TENANT;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

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
