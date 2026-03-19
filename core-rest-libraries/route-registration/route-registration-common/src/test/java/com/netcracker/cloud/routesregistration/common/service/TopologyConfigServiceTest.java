package com.netcracker.cloud.routesregistration.common.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.routesregistration.common.gateway.route.ServiceMeshType;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class TopologyConfigServiceTest {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    private TopologyConfigService service(String content) throws IOException {
        Path file = Files.createTempFile("topology", ".json");
        Files.writeString(file, content);
        file.toFile().deleteOnExit();
        return new TopologyConfigService(objectMapper, file.toString());
    }

    private TopologyConfigService serviceWithPath(String path) {
        return new TopologyConfigService(objectMapper, path);
    }

    @Test
    void defaultsToCoreWhenFileNotFound() {
        TopologyConfigService service = serviceWithPath("/nonexistent/topology.json");
        assertEquals(ServiceMeshType.CORE, service.getServiceMeshType());
    }

    @Test
    void defaultsToCoreWhenFileIsMalformed() throws IOException {
        TopologyConfigService service = service("not valid json");
        assertEquals(ServiceMeshType.CORE, service.getServiceMeshType());
    }

    @Test
    void defaultsToCoreWhenNodeIsMissing() throws IOException {
        TopologyConfigService service = service("{}");
        assertEquals(ServiceMeshType.CORE, service.getServiceMeshType());
    }

    @Test
    void defaultsToCoreWhenServiceMeshTypeIsNull() throws IOException {
        TopologyConfigService service = service("""
                {"featureFlags":{"core":{"serviceMeshType":null}}}
                """);
        assertEquals(ServiceMeshType.CORE, service.getServiceMeshType());
    }

    @Test
    void defaultsToCoreWhenServiceMeshTypeIsUnknown() throws IOException {
        TopologyConfigService service = service("""
                {"featureFlags":{"core":{"serviceMeshType":"UNKNOWN"}}}
                """);
        assertEquals(ServiceMeshType.CORE, service.getServiceMeshType());
    }

    @Test
    void returnsCoreWhenServiceMeshTypeIsCore() throws IOException {
        TopologyConfigService service = service("""
                {"featureFlags":{"core":{"serviceMeshType":"CORE"}}}
                """);
        assertEquals(ServiceMeshType.CORE, service.getServiceMeshType());
    }

    @Test
    void returnsIstioWhenServiceMeshTypeIsIstio() throws IOException {
        TopologyConfigService service = service("""
                {"featureFlags":{"core":{"serviceMeshType":"ISTIO"}}}
                """);
        assertEquals(ServiceMeshType.ISTIO, service.getServiceMeshType());
    }

    @Test
    void isCaseInsensitive() throws IOException {
        TopologyConfigService service = service("""
                {"featureFlags":{"core":{"serviceMeshType":"istio"}}}
                """);
        assertEquals(ServiceMeshType.ISTIO, service.getServiceMeshType());
    }

    @Test
    void cachesResultOnSubsequentCalls() throws IOException {
        TopologyConfigService service = service("""
                {"featureFlags":{"core":{"serviceMeshType":"ISTIO"}}}
                """);
        assertEquals(ServiceMeshType.ISTIO, service.getServiceMeshType());
        assertEquals(ServiceMeshType.ISTIO, service.getServiceMeshType());
    }
}
