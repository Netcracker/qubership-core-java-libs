package org.qubership.cloud.quarkus.stomp.ws.runtime.handlers;

import org.qubership.cloud.quarkus.stomp.ws.runtime.interceptors.SubscribeInterceptor;
import io.vertx.ext.stomp.DefaultSubscribeHandler;
import io.vertx.ext.stomp.ServerFrame;

public class SubscribeHandler  extends DefaultSubscribeHandler {

    private SubscribeInterceptor subscribeInterceptor;

    public SubscribeHandler (SubscribeInterceptor subscribeInterceptor){
        this.subscribeInterceptor = subscribeInterceptor;
    }

    public SubscribeHandler (){ }

    @Override
    public void handle(ServerFrame serverFrame) {
        if (subscribeInterceptor != null){
            subscribeInterceptor.preSubscribe(serverFrame);
        }
        super.handle(serverFrame);
        if (subscribeInterceptor != null){
            subscribeInterceptor.postSubscribe(serverFrame);
        }
    }
}
