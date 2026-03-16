package com.netcracker.cloud.routesregistration.common.gateway.route.rest;

import com.netcracker.cloud.routesregistration.common.gateway.route.RouteEntry;
import com.netcracker.cloud.routesregistration.common.gateway.route.Utils;
import com.netcracker.cloud.routesregistration.common.gateway.route.v3.RegistrationRequestV3Factory;
import lombok.RequiredArgsConstructor;

import java.util.Collection;

@RequiredArgsConstructor
public class RegistrationRequestFactory {
    private final RegistrationRequestV3Factory defaultRequestV3Factory;
    private final String microserviceInternalURL;
    private final String microserviceName;
    private final String deploymentVersion;
    private final String namespace;

    public RegistrationRequestFactory(String microserviceInternalURL,
                                      String microserviceName,
                                      String deploymentVersion,
                                      String cloudNamespace) {
        this.microserviceInternalURL = microserviceInternalURL;
        this.microserviceName = microserviceName;
        this.deploymentVersion = deploymentVersion;
        this.namespace = Utils.formatCloudNamespace(cloudNamespace);
        defaultRequestV3Factory = new RegistrationRequestV3Factory(microserviceInternalURL, microserviceName, deploymentVersion, namespace);
    }

    private AbstractRegistrationRequestFactory resolveFactory(String microserviceURL, ControlPlaneApiVersion targetApiVersion) {
        switch (targetApiVersion) {
            case V3:
                return microserviceInternalURL.equals(microserviceURL)
                        ? defaultRequestV3Factory
                        : new RegistrationRequestV3Factory(microserviceURL, microserviceName, deploymentVersion, namespace);
            default:
                throw new IllegalArgumentException("Unsupported control-plane API version: " + targetApiVersion);
        }
    }

    public final CompositeRequest<CommonRequest> createRequests(String microserviceURL,
                                                                Collection<RouteEntry> routes,
                                                                ControlPlaneApiVersion targetApiVersion) {
        return resolveFactory(microserviceURL, targetApiVersion).createRequests(routes);
    }

    public final CompositeRequest<CommonRequest> createRequests(Collection<RouteEntry> routes,
                                                                ControlPlaneApiVersion targetApiVersion) {
        return resolveFactory(microserviceInternalURL, targetApiVersion).createRequests(routes);
    }
}
