package org.qubership.cloud.routesregistration.common.config;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import org.qubership.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import org.qubership.cloud.routesregistration.common.gateway.route.*;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.RouteAnnotationProcessor;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.RouteFormatter;
import org.qubership.cloud.routesregistration.common.spring.gateway.route.SpringControlPlaneClient;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import java.util.List;
import java.util.Optional;

@Configuration
public class RouteRegistrationProcessorConfiguration {
    private final String microserviceName;
    private final String deploymentVersion;
    private String cloudServiceName;
    private final String cloudNamespace;
    private final String contextPath;
    private final String microservicePort;
    private final Boolean postRoutesAppnameDisabled;
    private final Boolean postRoutesEnabled;

    public RouteRegistrationProcessorConfiguration(@Value("${cloud.microservice.name}") String microserviceName,
                                                   @Value("${cloud.microservice.bg_version:}") String deploymentVersion,
                                                   @Value("${cloud.microservice.namespace}") String cloudNamespace,
                                                   @Value("${server.servlet.context-path:/}") String contextPath,
                                                   @Value("${server.port}") String microservicePort,
                                                   @Value("${apigateway.routes.registration.appname.disabled:false}") Boolean postRoutesAppnameDisabled,
                                                   @Value("${apigateway.routes.registration.enabled:true}") Boolean postRoutesEnabled) {
        this.microserviceName = microserviceName;
        this.deploymentVersion = deploymentVersion;
        this.cloudNamespace = cloudNamespace;
        this.contextPath = contextPath;
        this.microservicePort = microservicePort;
        this.postRoutesAppnameDisabled = postRoutesAppnameDisabled;
        this.postRoutesEnabled = postRoutesEnabled;

        this.cloudServiceName = microserviceName;
        if(deploymentVersion != null && !deploymentVersion.isEmpty()){
            this.cloudServiceName += "-" + deploymentVersion;
        }
    }

    private static final String DEFAULT_CONTROL_PLANE_URL = "http://control-plane:8080";
    private static final String GATEWAY_NAME_PROP = "mesh.gateway.name";
    private static final String GATEWAY_VIRTUAL_HOST_PROP = "mesh.gateway.virtualHosts";

    @Bean
    RouteAnnotationProcessor routeAnnotationProcessor(
            RouteFormatter routeFormatter,
            ApplicationContext applicationContext,
            Environment env) {
        RouteHostMapping mapper = new RouteHostMapping(env.getProperty(GATEWAY_NAME_PROP), env.getProperty(GATEWAY_VIRTUAL_HOST_PROP, List.class));
        return new RouteAnnotationProcessor(routeFormatter, applicationContext, mapper);
    }

    @Bean
    ControlPlaneClient controlPlaneClient(
            @Value("${apigateway.control-plane.url:#{null}}") Optional<String> controlPlaneUrl,
            @Qualifier("routeRegistrationRestClient") MicroserviceRestClient microserviceRestClient) {
        return new SpringControlPlaneClient(controlPlaneUrl.orElse(DEFAULT_CONTROL_PLANE_URL), microserviceRestClient);
    }

    @Bean
    RouteRetryManager routePostManager(Scheduler rxScheduler, RoutesRegistrationDelayProvider routesRegistrationDelayProvider) {
        return new RouteRetryManager(rxScheduler, routesRegistrationDelayProvider);
    }

    @Bean
    RegistrationRequestFactory registrationRequestFactory() {
        String microserviceInternalURL = Utils.formatMicroserviceInternalURL(
                cloudServiceName,
                microserviceName,
                microservicePort,
                contextPath,
                postRoutesAppnameDisabled
        );
        return new RegistrationRequestFactory(microserviceInternalURL, microserviceName, deploymentVersion, cloudNamespace);
    }

    @Bean
    RouteTransformer routeTransformer() {
        return new RouteTransformer(microserviceName);
    }

    @Bean
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor(ControlPlaneClient controlPlaneClient,
                                                                    RouteRetryManager routeRetryManager,
                                                                    RouteTransformer routeTransformer,
                                                                    RegistrationRequestFactory registrationRequestFactory) {
        String microserviceInternalURL = Utils.formatMicroserviceInternalURL(
                cloudServiceName,
                microserviceName,
                microservicePort,
                contextPath,
                postRoutesAppnameDisabled
        );
        return new RoutesRestRegistrationProcessor(
                controlPlaneClient,
                routeRetryManager,
                routeTransformer,
                registrationRequestFactory,
                postRoutesEnabled,
                microserviceName,
                microserviceInternalURL);
    }

    @Bean
    Scheduler rxScheduler() {
        return Schedulers.computation();
    }

    @Bean
    RouteFormatter routeFormatter() {
        return new RouteFormatter();
    }

    @Bean
    RoutesRegistrationDelayProvider routesRegistrationDelayProvider() {
        return new RoutesRegistrationDelayProvider();
    }
}
