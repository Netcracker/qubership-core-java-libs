package com.netcracker.cloud.consul.provider.common;

public interface ConsulLoginCredentials {

    String authMethod();

    String bearerToken();
}
