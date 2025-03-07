package org.qubership.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TxKVOperationWrapper {
    @JsonProperty("KV")
    TxKVOperation kv;
}
