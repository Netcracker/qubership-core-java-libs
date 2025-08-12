package com.netcracker.cloud.bluegreen.quarkus.config;

import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;
import org.jboss.logging.Logger;
import com.netcracker.cloud.bluegreen.api.model.BlueGreenState;
import com.netcracker.cloud.bluegreen.api.service.BlueGreenStatePublisher;

/**
 * Subscribes to Blue-Green state changes at application startup and updates a system property with the current state.
 * <p>
 * This class is a Quarkus {@code @ApplicationScoped} bean that listens to the {@link StartupEvent}
 * and registers a callback with the {@link BlueGreenStatePublisher} (if available).
 * Whenever the deployment state changes, the new state is written to a system property
 * defined by {@link #BG_STATE_SYSTEM_PROPERTY_KEY}.
 * </p>
 *
 * <p>
 * This system property (key: {@code "BG_STATE"}) can be used by logging frameworks or other components
 * that require access to the current deployment state but are not directly integrated with the
 * Blue-Green infrastructure.
 * </p>
 *
 * <p>Example usage in {@code application.properties}:</p>
 * <pre>{@code
 * quarkus.log.console.format=[%d{yyyy-MM-dd'T'HH:mm:ss.SSS}][%-5p] [bg_state:%#{BG_STATE:\-}] %s%e%n
 * }</pre>
 */
@ApplicationScoped
public class BGStateSubscriberConfiguration {
    public static final String BG_STATE_SYSTEM_PROPERTY_KEY = "BG_STATE";
    private static final Logger log = Logger.getLogger(BGStateSubscriberConfiguration.class);

    @Inject
    BlueGreenStatePublisher blueGreenStatePublisher;

    void onStart(@Observes StartupEvent ev) {
        if (blueGreenStatePublisher != null) {
            log.info("Subscribe to BlueGreenState change event => store BlueGreenState to the 'BG_STATE' System property");
            try {
                blueGreenStatePublisher.subscribe(this::setBGStateToSystemProperties);
            }
            catch (Exception e) {
                log.error("Cannot subscribe to BlueGreenStatePublisher for propagate Blue Green State to system properties " +
                          "=> 'BG_STATE' system property will be unavailable in logs", e);
            }
        } else {
            log.warn("Cannot get BlueGreenStatePublisher bean -> skip subscription");
        }
    }

    private synchronized void setBGStateToSystemProperties(BlueGreenState blueGreenState) {
        System.setProperty(BG_STATE_SYSTEM_PROPERTY_KEY, blueGreenState.getCurrent().getState().getName());
    }
}
