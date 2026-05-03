package com.netcracker.cloud.quarkus.framework.contexts.deployment;

import com.netcracker.cloud.framework.quarkus.contexts.allowedheaders.HeadersAllowedConfig;
import com.netcracker.cloud.framework.quarkus.contexts.allowedheaders.HeadersAllowedRecorder;
import io.quarkus.arc.deployment.UnremovableBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.deployment.builditem.ServiceStartBuildItem;

class FrameworkContextsQuarkusProcessor {

    private static final String FEATURE = "nc-framework-contexts-quarkus";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    UnremovableBeanBuildItem unremovableBeans() {
        return UnremovableBeanBuildItem.beanTypes(HeadersAllowedConfig.class);
    }

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServiceStartBuildItem fillSystemProperty(HeadersAllowedRecorder headersAllowedRecorder) {
        headersAllowedRecorder.setAllowedHeadersToSystemProperty();

        return new ServiceStartBuildItem("allowedHeadersRecord");
    }
}
