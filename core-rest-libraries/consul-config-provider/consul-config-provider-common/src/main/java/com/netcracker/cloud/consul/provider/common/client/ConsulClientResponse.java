package com.netcracker.cloud.consul.provider.common.client;

public class ConsulClientResponse {
    private final String bodyJson;
    private final int code;

    public ConsulClientResponse(String bodyJson, int code) {
        this.bodyJson = bodyJson;
        this.code = code;
    }

    public String getBodyJson() {
        return bodyJson;
    }

    public int getCode() {
        return code;
    }
}
