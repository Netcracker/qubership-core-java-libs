package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = RoutesTestConfiguration.class)
public class RouteFormatterTest {

    @Autowired
    RouteFormatter routeFormatter;

    @Test
    public void processRoute() throws Exception {
        /* pure route*/
        Assert.assertEquals("/api/v1/create", routeFormatter.processRoute("/api/v1/create"));

        /* route with path variable -> should be processed on gateway*/
        Assert.assertEquals("/api/v1/create/{tenantId}", routeFormatter.processRoute("/api/v1/create/{tenantId}"));

        /* routes with placeholders*/
        Assert.assertEquals("/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
               routeFormatter.processRoute("/${cloud.microservice.name}/create"));

        Assert.assertEquals("/api/v1/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/api/v1/${cloud.microservice.name}/create"));

        Assert.assertEquals("/"+ RoutesTestConfiguration.SPRING_CLOUD_CONFIG_URI_VALUE + "/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${spring.cloud.config.uri}/${cloud.microservice.name}/create"));

        Assert.assertEquals("/"+ RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${cloud.microservice.name}/${cloud.microservice.name}/create"));

        Assert.assertEquals("/"+ RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/"+ RoutesTestConfiguration.SPRING_CLOUD_CONFIG_URI_VALUE + "/" + RoutesTestConfiguration.SPRING_APPLICATION_NAME_VALUE + "/create",
                routeFormatter.processRoute("/${cloud.microservice.name}/${spring.cloud.config.uri}/${cloud.microservice.name}/create"));
    }

}