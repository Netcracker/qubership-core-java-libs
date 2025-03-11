package org.qubership.cloud.quarkus.disableapi.deployment;

import org.qubership.cloud.disableapi.quarkus.DisableApiFilter;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

public class DisableDeprecatedApiFilterProviderFeature {
    private static final String FEATURE = "rest-api-deprecation-switcher";
    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    AdditionalBeanBuildItem registerAdditionalBeans() {
        return new AdditionalBeanBuildItem.Builder()
                .setUnremovable()
                .addBeanClasses(DisableApiFilter.class)
                .build();
    }
}
