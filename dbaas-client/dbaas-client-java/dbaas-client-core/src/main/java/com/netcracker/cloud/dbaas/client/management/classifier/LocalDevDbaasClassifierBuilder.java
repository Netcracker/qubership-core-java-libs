package org.qubership.cloud.dbaas.client.management.classifier;

import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.dbaas.client.management.DbaasDbClassifier;

public class LocalDevDbaasClassifierBuilder extends DbaaSChainClassifierBuilder {
    private static final String LOCAL_DEV_KEY = "localdev";

    private final MSInfoProvider msInfoProvider;

    public LocalDevDbaasClassifierBuilder(MSInfoProvider msInfoProvider, DbaaSChainClassifierBuilder next) {
        super(next);
        this.msInfoProvider = msInfoProvider;
    }

    @Override
    public DbaasDbClassifier build() {
        if (msInfoProvider.getLocalDevNamespace() != null) {
            return new DbaasDbClassifier.Builder()
                    .withProperties(super.build().asMap())
                    .withProperty(LOCAL_DEV_KEY, msInfoProvider.getLocalDevNamespace())
                    .build();
        }
        return super.build();
    }
}
