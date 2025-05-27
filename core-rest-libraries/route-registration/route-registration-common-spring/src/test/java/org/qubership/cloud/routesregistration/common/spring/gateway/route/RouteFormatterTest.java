package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertEquals;

@SpringBootTest(classes = RoutesTestConfiguration.class)
class RouteFormatterTest {

    @Autowired
    RouteFormatter routeFormatter;

    @Test
    void processRoute() {
        /* pure route*/
        assertEquals("/api/v1/create", routeFormatter.processRoute("/api/v1/create"));

        /* route with path variable -> should be processed on gateway*/
        assertEquals("/api/v1/create/{tenantId}", routeFormatter.processRoute("/api/v1/create/{tenantId}"));

        /* routes with placeholders*/
        assertEquals("/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${cloud.microservice.name}/create"));

        assertEquals("/api/v1/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/api/v1/${cloud.microservice.name}/create"));

        assertEquals("/" + RoutesTestConfiguration.SPRING_CLOUD_CONFIG_URI_VALUE + "/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${spring.cloud.config.uri}/${cloud.microservice.name}/create"));

        assertEquals("/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${cloud.microservice.name}/${cloud.microservice.name}/create"));

        assertEquals("/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/" + RoutesTestConfiguration.SPRING_CLOUD_CONFIG_URI_VALUE + "/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${cloud.microservice.name}/${spring.cloud.config.uri}/${cloud.microservice.name}/create"));
    }
}
