package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.gateway.route.RouteEntry;
import org.qubership.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationListener;
import org.springframework.context.event.ContextRefreshedEvent;

import java.util.Collection;

@Slf4j
@RequiredArgsConstructor
public class RoutesRegistrationApplicationListener implements ApplicationListener<ContextRefreshedEvent> {

    private final RouteAnnotationProcessor routeAnnotationProcessor;
    private final RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    @Override
    public void onApplicationEvent(final ContextRefreshedEvent event) {
        try {
            final Collection<RouteEntry> routes = routeAnnotationProcessor.scanForRoutes();
            if (!routes.isEmpty()) {
                routesRestRegistrationProcessor.postRoutes(routes);
            }
        } catch (Exception ex) {
            log.error("Microservice routes posting failed", ex);
            throw new RuntimeException(ex);
        }
    }
}
