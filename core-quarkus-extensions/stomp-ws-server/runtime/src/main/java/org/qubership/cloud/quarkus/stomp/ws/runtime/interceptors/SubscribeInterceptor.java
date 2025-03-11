package org.qubership.cloud.quarkus.stomp.ws.runtime.interceptors;

import io.vertx.ext.stomp.ServerFrame;

public interface SubscribeInterceptor {
    void preSubscribe(ServerFrame serverFrame);
    void postSubscribe(ServerFrame serverFrame);
}
