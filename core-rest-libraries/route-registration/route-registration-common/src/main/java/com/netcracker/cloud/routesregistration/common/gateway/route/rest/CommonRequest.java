package com.netcracker.cloud.routesregistration.common.gateway.route.rest;

public interface CommonRequest {
    Object getPayload();

    String getUrl();

    String getMethod();

    int getPriority();
}
