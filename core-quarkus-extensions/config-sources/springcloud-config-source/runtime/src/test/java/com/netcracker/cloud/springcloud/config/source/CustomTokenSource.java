package com.netcracker.cloud.springcloud.config.source;

import com.netcracker.cloud.security.core.utils.k8s.Priority;
import com.netcracker.cloud.security.core.utils.k8s.TokenSource;

@Priority(10)
public class CustomTokenSource implements TokenSource {

    public String getToken(String audience) {
        return "test-token";
    }

    @Override
    public void close() throws Exception {
    }
}
