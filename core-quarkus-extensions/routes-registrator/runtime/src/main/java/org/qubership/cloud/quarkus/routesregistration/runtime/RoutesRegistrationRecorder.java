package org.qubership.cloud.quarkus.routesregistration.runtime;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.annotations.Recorder;

import java.util.Collection;

@Recorder
public class RoutesRegistrationRecorder {
    public void register(Collection<RouteEntry> routes) {
        if (routes.isEmpty()) {
            return;
        }

        InstanceHandle<RoutesRestRegistrationProcessor> routesRestRegistrationProcessor = Arc.container().instance(RoutesRestRegistrationProcessor.class);
        if (routesRestRegistrationProcessor.isAvailable()) {
            routesRestRegistrationProcessor.get().postRoutes(routes);
        }
    }
}
