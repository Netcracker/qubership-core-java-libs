package com.netcracker.cloud.dbaas.common.mountedsecret;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Descriptor read from a mounted Secret's {@code metadata.json}. Mirrors the operator's
 * descriptor (Part I of the design); unknown fields are ignored for forward compatibility.
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class SecretMetadata {
    private Map<String, Object> classifier;
    private String type;
    private String userRole;
    private String id;
    private String name;
    private String namespace;
    private Map<String, Object> settings;
}
