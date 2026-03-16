package com.netcracker.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netcracker.cloud.bluegreen.impl.dto.serdes.Base64Deserializer;
import com.netcracker.cloud.bluegreen.impl.dto.serdes.Base64Serializer;
import lombok.Builder;
import lombok.Data;

@Builder
@Data
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxKVOperation {
    @JsonProperty("Verb")
    TxnKVVerb verb;

    @JsonProperty("Key")
    String key;

    @JsonProperty("Value")
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    String value;

    @JsonProperty("Flags")
    Integer flags;

    @JsonProperty("Index")
    Long index;

    @JsonProperty("Session")
    String session;

}
