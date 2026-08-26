package com.netcracker.cloud.consul.provider.common;

import java.io.IOException;

public interface ConsulLogin {

    Token perform() throws IOException;
}
