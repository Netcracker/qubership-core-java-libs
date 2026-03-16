package com.netcracker.cloud.routesregistration.common.gateway.route;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.util.List;

/**
 * Deprecated API V1 request model.
 */
@Deprecated
@Data
public class RouteEntityRequest {

    @NotNull(message = "Microservice url must be specified")
    private String microserviceUrl;

    @NotNull(message = "Route list can't be null")
    private List<RouteEntry> routes;

    @NotNull(message = "Routes marker must be specified")
    private boolean allowed = true;

    @NotNull(message = "Microservice name must be specified")
    private String microserviceName;

    @NotNull(message = "Microservice IP must be specified")
    private String microserviceIp;

}
