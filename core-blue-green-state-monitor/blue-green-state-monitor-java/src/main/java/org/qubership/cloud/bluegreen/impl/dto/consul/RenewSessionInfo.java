package org.qubership.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class RenewSessionInfo {
    @JsonProperty("ID")
    private String id;

    @JsonProperty("Name")
    private String name;

    @JsonProperty("TTL")
    private String ttl;

    @JsonProperty("ModifyIndex")
    private String modifyIndex;
}
