package com.netcracker.cloud.consul.provider.common;

import java.io.IOException;

public interface ConsulTokenProvider {

    Token getToken() throws IOException;
}
