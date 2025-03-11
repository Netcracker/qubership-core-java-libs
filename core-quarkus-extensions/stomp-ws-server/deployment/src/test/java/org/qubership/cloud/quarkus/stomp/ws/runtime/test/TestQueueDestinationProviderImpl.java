package org.qubership.cloud.quarkus.stomp.ws.runtime.test;

import org.qubership.cloud.quarkus.stomp.ws.runtime.DestinationProvider;
import io.vertx.ext.stomp.Destination;
import jakarta.enterprise.context.ApplicationScoped;


@ApplicationScoped
public class TestQueueDestinationProviderImpl extends DestinationProvider {

    public static final String USER_DESTINATION_PREFIX = "/channels";

    private Destination destination;

    @Override
    public String getDestinationPath() {
        return USER_DESTINATION_PREFIX;
    }

    @Override
    public void setDestination(Destination destination) {
        this.destination = destination;
    }

    @Override
    public Destination getDestination() {
        return destination;
    }

    @Override
    public DestinationType getDestinationType() {
        return DestinationType.QUEUE;
    }

    @Override
    public boolean isDestinationSetUp() {
        return destination != null;
    }

}
