package com.netcracker.cloud.quarkus.stomp.ws.server.deployment;

import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;
import io.smallrye.config.WithName;

@ConfigMapping(prefix = "quarkus.stomp-server")
@ConfigRoot
public interface StompServerConfig {

    /**
     * Endpoint by which stomp ws client will init connect
     */
    @WithDefault("/stomp")
    String websocketPath();

    /**
     * Run server either over sockjs or over standard websocket protocol
     */
    @WithName("isSockJS")
    @WithDefault("true")
    boolean isSockJS();
}
