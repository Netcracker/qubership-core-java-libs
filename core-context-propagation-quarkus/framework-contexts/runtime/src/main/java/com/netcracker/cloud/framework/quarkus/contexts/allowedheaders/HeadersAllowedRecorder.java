package org.qubership.cloud.framework.quarkus.contexts.allowedheaders;

import io.quarkus.runtime.annotations.Recorder;

@Recorder
public class HeadersAllowedRecorder {

    public void setAllowedHeadersToSystemProperty(HeadersAllowedConfig allowedConfig) {
        allowedConfig.allowedHeaders.ifPresent(allowedHeaders -> System.setProperty("headers.allowed", allowedHeaders));
    }
}
