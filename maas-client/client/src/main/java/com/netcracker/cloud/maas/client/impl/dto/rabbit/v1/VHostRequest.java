package com.netcracker.cloud.maas.client.impl.dto.rabbit.v1;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.netcracker.cloud.maas.client.api.Classifier;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_EMPTY)
public class VHostRequest {
    Classifier classifier;
}
