package org.qubership.cloud.dbaas.client.management.classifier;

import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;
import lombok.RequiredArgsConstructor;

import java.util.Map;

@RequiredArgsConstructor
public class DbaasClassifierFactory {
    private final MSInfoProvider msInfoProvider;

    public DbaaSChainClassifierBuilder newTenantClassifierBuilder() {
        return new TenantDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, null));
    }

    public DbaaSChainClassifierBuilder newServiceClassifierBuilder() {
        return new ServiceDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, null));
    }

    public DbaaSChainClassifierBuilder newTenantClassifierBuilder(DbaaSChainClassifierBuilder dbaaSChainClassifierBuilder) {
        return new TenantDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, dbaaSChainClassifierBuilder));
    }

    public DbaaSChainClassifierBuilder newServiceClassifierBuilder(DbaaSChainClassifierBuilder dbaaSChainClassifierBuilder) {
        return new ServiceDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, dbaaSChainClassifierBuilder));
    }

    public DbaaSChainClassifierBuilder newTenantClassifierBuilder(Map<String, Object> additionalFields) {
        if (additionalFields == null || additionalFields.isEmpty()) {
            return new TenantDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, null));
        } else {
            DbaasDbClassifier.Builder classifierBuilderExtension = new DbaasDbClassifier.Builder();
            additionalFields.forEach(classifierBuilderExtension::withProperty);
            return new TenantDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, null))
                    .pass(classifierBuilderExtension);
        }
    }

    public DbaaSChainClassifierBuilder newServiceClassifierBuilder(Map<String, Object> additionalFields) {
        if (additionalFields == null || additionalFields.isEmpty()) {
            return new ServiceDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, null));
        } else {
            DbaasDbClassifier.Builder classifierBuilderExtension = new DbaasDbClassifier.Builder();
            additionalFields.forEach(classifierBuilderExtension::withProperty);
            return new ServiceDbaaSClassifierBuilder(new LocalDevDbaasClassifierBuilder(msInfoProvider, null))
                    .pass(classifierBuilderExtension);
        }
    }
}
