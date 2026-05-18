package com.netcracker.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HeadersAllowedRecorder {

    public void setAllowedHeadersToSystemProperty() {
        HeadersAllowedConfig allowedConfig = Arc.container().instance(HeadersAllowedConfig.class).get();

        allowedConfig.allowedHeaders()
                .ifPresent(allowedHeaders -> System.setProperty("headers.allowed", allowedHeaders));

        allowedConfig.allowedHeadersFromBlocklist()
                .filter(list -> !list.isEmpty())
                .ifPresent(list -> System.setProperty("context.propagation.allow-blocked-headers", String.join(",", list)));
    }
}
