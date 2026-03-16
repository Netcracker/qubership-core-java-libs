package com.netcracker.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class TxKVOperationResult {
    @JsonProperty("ModifyIndex")
    Long modifyIndex;
}
