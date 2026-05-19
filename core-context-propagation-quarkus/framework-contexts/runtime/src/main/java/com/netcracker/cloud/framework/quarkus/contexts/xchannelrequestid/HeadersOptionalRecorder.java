package com.netcracker.cloud.framework.quarkus.contexts.xchannelrequestid;

import com.netcracker.cloud.framework.contexts.xchannelrequestid.HeaderPropagationConfiguration;

import io.quarkus.arc.Arc;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Bridges {@link HeadersOptionalConfig} to the JVM system property
 * {@link HeaderPropagationConfiguration#ENABLE_OPTIONAL_PROPERTY} which the non-Quarkus
 * framework reads to compute the effective restricted list.
 */
@Recorder
public class HeadersOptionalRecorder {

    public void setEnableOptionalToSystemProperty() {
        HeadersOptionalConfig config = Arc.container().instance(HeadersOptionalConfig.class).get();

        config.enableOptional()
                .filter(list -> !list.isEmpty())
                .ifPresent(list -> System.setProperty(
                        HeaderPropagationConfiguration.ENABLE_OPTIONAL_PROPERTY,
                        String.join(",", list)));
    }
}
