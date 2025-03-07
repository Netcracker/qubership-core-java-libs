package org.qubership.cloud.maas.client.impl.dto.conf;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConfigResource<S> {
    String apiVersion;
    String kind;
    Map<String, String> pragma;
    S spec;
}
