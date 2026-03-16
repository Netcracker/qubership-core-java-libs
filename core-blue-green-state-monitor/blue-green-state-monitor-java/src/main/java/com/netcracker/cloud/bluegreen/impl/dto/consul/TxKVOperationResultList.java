package com.netcracker.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TxKVOperationResultList {
    @JsonProperty("Results")
    List<TxKVOperationResultWrapper> results;

    @JsonProperty("Errors")
    List<TxKVOperationError> errors;
}
