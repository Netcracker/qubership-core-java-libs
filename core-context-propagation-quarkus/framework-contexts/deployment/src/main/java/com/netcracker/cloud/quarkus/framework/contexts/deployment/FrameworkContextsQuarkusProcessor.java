package com.netcracker.cloud.quarkus.framework.contexts.deployment;

import com.netcracker.cloud.framework.quarkus.contexts.allowedheaders.HeadersAllowedConfig;
import com.netcracker.cloud.framework.quarkus.contexts.allowedheaders.HeadersAllowedRecorder;
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

    @Record(ExecutionTime.RUNTIME_INIT)
    @BuildStep
    ServiceStartBuildItem fillSystemProperty(HeadersAllowedRecorder headersAllowedRecorder, HeadersAllowedConfig config) {
        headersAllowedRecorder.setAllowedHeadersToSystemProperty(config);

        return new ServiceStartBuildItem("allowedHeadersRecord");
    }
}
