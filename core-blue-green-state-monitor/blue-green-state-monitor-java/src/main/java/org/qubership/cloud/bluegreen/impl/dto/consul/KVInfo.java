package org.qubership.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.qubership.cloud.bluegreen.impl.dto.serdes.Base64Deserializer;
import org.qubership.cloud.bluegreen.impl.dto.serdes.Base64Serializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class KVInfo {
    @JsonProperty("Key")
    String key;

    @JsonProperty("Value")
    @JsonSerialize(using = Base64Serializer.class)
    @JsonDeserialize(using = Base64Deserializer.class)
    String value;

    @JsonProperty("ModifyIndex")
    String modifyIndex;
}
