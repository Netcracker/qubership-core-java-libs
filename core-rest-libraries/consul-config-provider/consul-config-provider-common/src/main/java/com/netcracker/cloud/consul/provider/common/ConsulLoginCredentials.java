package com.netcracker.cloud.consul.provider.common;

public interface ConsulLoginCredentials {

    String getAuthMethod();

    String getBearerToken();
}
