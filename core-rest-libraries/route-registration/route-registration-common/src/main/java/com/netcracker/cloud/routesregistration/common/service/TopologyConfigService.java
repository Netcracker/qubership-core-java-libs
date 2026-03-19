package com.netcracker.cloud.routesregistration.common.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.routesregistration.common.gateway.route.ServiceMeshType;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Optional;

@Slf4j
public class TopologyConfigService {
    private static final String TOPOLOGY_CONFIG_PATH_DEFAULT = "/etc/topology.json";
    private static final String SERVICE_MESH_TYPE_PATH = "/featureFlags/core/serviceMeshType";

    private final ObjectMapper objectMapper;
    private final String topologyConfigPath;

    private volatile ServiceMeshType serviceMeshType;

    public TopologyConfigService(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
        this.topologyConfigPath = Optional.ofNullable(System.getenv("TOPOLOGY_CONFIG_PATH"))
                .filter(s -> !s.isBlank())
                .orElse(TOPOLOGY_CONFIG_PATH_DEFAULT);
    }

    // visible for testing
    TopologyConfigService(ObjectMapper objectMapper, String topologyConfigPath) {
        this.objectMapper = objectMapper;
        this.topologyConfigPath = topologyConfigPath;
    }

    public ServiceMeshType getServiceMeshType() {
        if (serviceMeshType != null) {
            return serviceMeshType;
        }
        synchronized (this) {
            if (serviceMeshType != null) {
                return serviceMeshType;
            }
            serviceMeshType = loadServiceMeshType();
        }
        return serviceMeshType;
    }

    private ServiceMeshType loadServiceMeshType() {
        try (InputStream inputStream = Files.newInputStream(Paths.get(topologyConfigPath))) {
            JsonNode root = objectMapper.readTree(inputStream);
            JsonNode node = root.at(SERVICE_MESH_TYPE_PATH);

            if (node.isMissingNode() || node.isNull()) {
                log.warn("'{}' not found in topology config, defaulting to {}", SERVICE_MESH_TYPE_PATH, ServiceMeshType.CORE);
                return ServiceMeshType.CORE;
            }

            return parseServiceMeshType(node.asText());

        } catch (IOException e) {
            log.warn("Failed to read topology config from '{}', defaulting to {}", topologyConfigPath, ServiceMeshType.CORE, e);
            return ServiceMeshType.CORE;
        }
    }

    private ServiceMeshType parseServiceMeshType(String value) {
        return Arrays.stream(ServiceMeshType.values())
                .filter(type -> type.name().equalsIgnoreCase(value))
                .findFirst()
                .orElseGet(() -> {
                    log.warn("Unknown serviceMeshType '{}', defaulting to {}", value, ServiceMeshType.CORE);
                    return ServiceMeshType.CORE;
                });
    }
}
