package org.qubership.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;

@Builder
public class CreateSession {
    @JsonProperty("Behavior")
    private Behavior behavior;

    @JsonProperty("TTL")
    private String ttl;

    public enum Behavior {
        delete, release
    }
}
