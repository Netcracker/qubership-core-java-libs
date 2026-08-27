package com.netcracker.cloud.consul.provider.common;

//todo vlla может использовать get в именах методов?
public interface ConsulLoginCredentials {

    String authMethod();

    String bearerToken();
}
