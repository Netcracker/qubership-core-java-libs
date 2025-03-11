package org.qubership.cloud.quarkus.stomp.ws.server.deployment;

import io.quarkus.runtime.annotations.ConfigItem;
import io.quarkus.runtime.annotations.ConfigRoot;

@ConfigRoot
public class StompServerConfig {

    /**
     * Endpoint by which stomp ws client will init connect
     */
    @ConfigItem(defaultValue = "/stomp")
    String websocketPath;

    /**
     * Run server either over sockjs or over standard websocket protocol
     */
    @ConfigItem(name = "isSockJS", defaultValue = "true")
    boolean isSockJS;
}