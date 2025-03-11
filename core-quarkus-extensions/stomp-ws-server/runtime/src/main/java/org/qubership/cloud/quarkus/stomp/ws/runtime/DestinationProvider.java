package org.qubership.cloud.quarkus.stomp.ws.runtime;

import io.quarkus.runtime.StartupEvent;
import io.vertx.ext.stomp.Destination;
import jakarta.enterprise.event.Observes;
import org.jboss.logging.Logger;

public abstract class DestinationProvider {

    private static final Logger log = Logger.getLogger(DestinationProvider.class.getName());

    abstract public String getDestinationPath();

    abstract public void setDestination(Destination destination);

    abstract public Destination getDestination();

    abstract public DestinationType getDestinationType();

    abstract public boolean isDestinationSetUp();

    public static enum DestinationType {
        TOPIC, QUEUE
    }

    public void init(@Observes StartupEvent event) {
        log.debug("Destination provider started: " + this.getClass().getSimpleName());
    }

}
