package org.qubership.cloud.maas.client.impl.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.List;

@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiVersionResponse {
    int major;
    int minor;
    List<Spec> specs;

    @Data
    public static class Spec {
        String specRootUrl;
        int major;
        int minor;
    }
}
