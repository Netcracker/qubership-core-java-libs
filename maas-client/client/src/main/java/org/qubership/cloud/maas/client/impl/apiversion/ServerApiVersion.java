package org.qubership.cloud.maas.client.impl.apiversion;

import org.qubership.cloud.maas.client.impl.Lazy;
import org.qubership.cloud.maas.client.impl.dto.ApiVersionResponse;
import org.qubership.cloud.maas.client.impl.http.HttpClient;

import java.util.Optional;

public class ServerApiVersion {
    final Lazy<ApiVersion> serverApiVersion;

    public ServerApiVersion(HttpClient httpClient, String maasAgentUrl) {
        this.serverApiVersion = new Lazy<>(() ->
                httpClient.request(maasAgentUrl + "/api-version")
                        .expect(200)
                        .sendAndReceive(ApiVersionResponse.class)
                        .map(ServerApiVersion::convertResponse)
                        .get()
        );
    }

    public void requiresApiVersion(int major, int minor) {
        var minimal = new ApiVersion(major, minor);
        if (!minimal.isCompatible(serverApiVersion.get())) {
            throw new RuntimeException(
                    String.format("MaaS API not compatible with requested feature. Required min API version: %s, but MaaS server API version is: %s", minimal, serverApiVersion.get())
            );
        }
    }

    public boolean isCompatible(int major, int minor) {
        return new ApiVersion(major, minor).isCompatible(serverApiVersion.get());
    }

    private static ApiVersion convertResponse(ApiVersionResponse response) {
        return Optional.ofNullable(response.getSpecs()).flatMap(
                specs -> specs.stream()
                        .filter(o -> "/api".equals(o.getSpecRootUrl()))
                        .findFirst()
                        .map(spec -> new ApiVersion(spec.getMajor(), spec.getMinor()))
                )
                .orElse(new ApiVersion(response.getMajor(), response.getMinor()));
    }
}
