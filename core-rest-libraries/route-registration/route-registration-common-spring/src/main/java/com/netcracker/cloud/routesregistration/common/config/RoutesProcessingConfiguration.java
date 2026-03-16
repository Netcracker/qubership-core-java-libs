package com.netcracker.cloud.routesregistration.common.config;

import com.netcracker.cloud.routesregistration.common.gateway.route.RoutesRestRegistrationProcessor;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessor;
import com.netcracker.cloud.routesregistration.common.spring.gateway.route.RoutesRegistrationApplicationListener;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

@Configuration
@Import(RouteRegistrationProcessorConfiguration.class)
public class RoutesProcessingConfiguration {
    private final RouteAnnotationProcessor routeAnnotationProcessor;
    private final RoutesRestRegistrationProcessor routesRestRegistrationProcessor;

    public RoutesProcessingConfiguration(RouteAnnotationProcessor routeAnnotationProcessor,
                                                             RoutesRestRegistrationProcessor routesRestRegistrationProcessor) {
        this.routeAnnotationProcessor = routeAnnotationProcessor;
        this.routesRestRegistrationProcessor = routesRestRegistrationProcessor;
    }

    @Bean
    RoutesRegistrationApplicationListener routesRegistrationApplicationListener() {
        return new RoutesRegistrationApplicationListener(routeAnnotationProcessor, routesRestRegistrationProcessor);
    }
}
