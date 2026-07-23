package com.netcracker.cloud.dbaas.client.service.mountedsecret;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.Data;

import java.util.Map;

/**
 * Descriptor that dbaas-operator writes into the {@code metadata.json} key of every Secret it
 * materializes for a {@code DatabaseSecretClaim}. It mirrors the operator's {@code secretMetadata}
 * struct and the Go client's {@code secretMetadata}.
 * <p>
 * Unknown properties are ignored on purpose so a newer operator (e.g. a future {@code schemaVersion})
 * does not break an older client.
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
