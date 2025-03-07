package org.qubership.cloud.routesregistration.common.spring.gateway.route;

import org.qubership.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

@Configuration
public class RoutesTestConfigurationWithoutDefaultMapping extends RoutesTestConfiguration {
    @Bean
    RouteAnnotationProcessor routeAnnotationProcessor(
            RouteFormatter routeFormatter,
            ApplicationContext applicationContext,
            Environment env) {
        return new RouteAnnotationProcessor(routeFormatter, applicationContext, new RouteHostMapping());
    }
}
