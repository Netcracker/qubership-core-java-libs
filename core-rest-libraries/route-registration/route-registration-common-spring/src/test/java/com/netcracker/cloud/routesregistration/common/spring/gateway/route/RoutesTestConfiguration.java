package com.netcracker.cloud.routesregistration.common.spring.gateway.route;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.resttemplate.MicroserviceRestTemplate;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.PostConstruct;
import org.mockito.Mockito;
import com.netcracker.cloud.routesregistration.common.annotation.processing.RouteHostMapping;
import com.netcracker.cloud.routesregistration.common.gateway.route.*;
import com.netcracker.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import com.netcracker.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.core.env.Environment;
import org.springframework.validation.beanvalidation.MethodValidationPostProcessor;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Properties;

@Configuration
public class RoutesTestConfiguration {

    static final String CLASS_ROUTES_1 = "/api/v1/test1";
    static final String CLASS_ROUTES_2 = "/api/v1/test2";
    static final String CLASS_ROUTES_3 = "/api/v1/test3";
    static final String CLASS_ROUTES_4 = "/api/v1/test4";
    static final String CLASS_ROUTES_8 = "/api/v1/test8";
    static final String CLASS_ROUTES_10 = "/api/v1/test10";
    static final String CLASS_ROUTES_11 = "/api/v1/test11";
    static final String CLASS_ROUTES_12 = "/api/v1/test12";
    static final String METHOD_ROUTES_1 = "/test1";
    static final String METHOD_ROUTES_2 = "/test2";
    static final String METHOD_ROUTES_3 = "/test3";
    static final String CLASS_ROUTE_PATH_FROM_1 = "/class/path/from";
    static final String METHOD_ROUTE_PATH_FROM_1 = "/method/path/from";
    static final String CLASS_ROUTE_PATH_TO_1 = "/class/path/to";
    static final String METHOD_ROUTE_PATH_TO_1 = "/method/path/to";
    static final String CLASS_ROUTE_PATH_FROM_2 = "/class/path/from2";
    static final String METHOD_ROUTE_PATH_FROM_2 = "/method/path/from2";
    static final String METHOD_ROUTE_PATH_TO_2 = "/method/path/to2";

    public static final String CLOUD_MICROSERVICE_NAME = "cloud.microservice.name";
    public static final String SPRING_CLOUD_CONFIG_URI = "spring.cloud.config.uri";
    public static final String SPRING_APPLICATION_NAME_VALUE = "RoutesTestConfiguration";
    public static final String SPRING_CLOUD_CONFIG_URI_VALUE = "http:localhost:8888";

    public static final String ROUTES_REGISTRATION_URL = "/api/v2/control-plane/routes";
    public static final String CONTROL_PLANE_URL = "http://control-plane:8080";
    public static final String INTERNAL_NODE_GROUP = "internal-gateway-service";
    public static final String PUBLIC_NODE_GROUP = "public-gateway-service";
    public static final String PRIVATE_NODE_GROUP = "private-gateway-service";
    public static final String MICROSERVICE_TEST_NAME = "ms-core-test";
    public static final String PORT = "8080";
    public static final String CONTEXT_PATH = "/contextPath";
    public static final String MICROSERVICE_URL = MICROSERVICE_TEST_NAME + ":" + PORT + CONTEXT_PATH;
    public static final String INGRESS_GATEWAY = "ingress-gateway";

    public static final int CORES_NUM = Runtime.getRuntime().availableProcessors();
    public static final int THREADS_NUM = CORES_NUM;
    public static final String APIGATEWAY_ROUTES_REGISTRATION_URL = "apigateway.routes.registration.url";
    public static final String APIGATEWAY_CONTROL_PLANE_URL = "apigateway.nodegroup.private";
    public static final String SERVER_PORT = "server.port";
    public static final String SERVER_CONTEXT_PATH = "server.servlet.context-path";
    public static final String DEFAULT_INGRESS_GATEWAY = "default-ingress-gateway";
    public static final String DEFAULT_VHOST = "default-vhost";

    static List<RouteEntry> ROUTES_LIST;

    public static final long TEST_TIMEOUT_1 = 150000;
    public static final long TEST_TIMEOUT_2 = 250000;

    @PostConstruct
    void setProperties() {
        ROUTES_LIST = new ArrayList<>(22);
        /* Routes for TestController1*/
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_2, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_1 + METHOD_ROUTES_1, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_1 + METHOD_ROUTES_2, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_2 + METHOD_ROUTES_1, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_2 + METHOD_ROUTES_2, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_1 + METHOD_ROUTES_1 + METHOD_ROUTES_2, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTES_2 + METHOD_ROUTES_1 + METHOD_ROUTES_2, RouteType.INTERNAL));

        /* Routes for TestController2*/
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTES_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTES_2, RouteType.INTERNAL));
        /*repeated routes*/
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTES_1 + METHOD_ROUTES_2, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTES_1 + METHOD_ROUTES_2, RouteType.PRIVATE));

        /* Routes for TestController3*/
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTE_PATH_FROM_1, CLASS_ROUTE_PATH_TO_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTE_PATH_FROM_2, CLASS_ROUTE_PATH_TO_1, RouteType.INTERNAL));

        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTE_PATH_FROM_1 + METHOD_ROUTE_PATH_FROM_1, CLASS_ROUTE_PATH_TO_1 + METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTE_PATH_FROM_1 + METHOD_ROUTE_PATH_FROM_2, CLASS_ROUTE_PATH_TO_1 + METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));

        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTE_PATH_FROM_2 + METHOD_ROUTE_PATH_FROM_1, CLASS_ROUTE_PATH_TO_1 + METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));
        ROUTES_LIST.add(new RouteEntry(CLASS_ROUTE_PATH_FROM_2 + METHOD_ROUTE_PATH_FROM_2, CLASS_ROUTE_PATH_TO_1 + METHOD_ROUTE_PATH_TO_1, RouteType.INTERNAL));

        /* Routes for TestController4*/
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTE_PATH_FROM_1, METHOD_ROUTE_PATH_TO_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTE_PATH_FROM_2, METHOD_ROUTE_PATH_TO_1, RouteType.PUBLIC));
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTE_PATH_FROM_1 + METHOD_ROUTE_PATH_FROM_1, METHOD_ROUTE_PATH_TO_2, RouteType.PRIVATE));
        ROUTES_LIST.add(new RouteEntry(METHOD_ROUTE_PATH_FROM_2 + METHOD_ROUTE_PATH_FROM_2, METHOD_ROUTE_PATH_TO_2, RouteType.PRIVATE));

        /* Routes for TestControllerExtended */
    }

    @Bean
    public static PropertySourcesPlaceholderConfigurer properties() {
        final PropertySourcesPlaceholderConfigurer pspc = new PropertySourcesPlaceholderConfigurer();
        final Properties properties = new Properties();
        properties.setProperty(CLOUD_MICROSERVICE_NAME, SPRING_APPLICATION_NAME_VALUE);
        properties.setProperty(SPRING_CLOUD_CONFIG_URI, SPRING_CLOUD_CONFIG_URI_VALUE);
        properties.setProperty(APIGATEWAY_ROUTES_REGISTRATION_URL, ROUTES_REGISTRATION_URL);
        properties.setProperty(APIGATEWAY_CONTROL_PLANE_URL, CONTROL_PLANE_URL);
        properties.setProperty(SERVER_PORT, PORT);
        properties.setProperty(SERVER_CONTEXT_PATH, CONTEXT_PATH);
        pspc.setProperties(properties);
        pspc.setIgnoreResourceNotFound(true);
        return pspc;
    }

    @Bean(name = "msCoreRestTemplate")
    RestTemplate restTemplate() {
        return Mockito.mock(RestTemplate.class);
    }

    @Bean
    MicroserviceRestClient microserviceRestClient() {
        return new MicroserviceRestTemplate();
    }

    @Bean
    RouteAnnotationProcessor routeAnnotationProcessor(
            RouteFormatter routeFormatter,
            ApplicationContext applicationContext,
            Environment env) {
        return new RouteAnnotationProcessor(routeFormatter, applicationContext, new RouteHostMapping(DEFAULT_INGRESS_GATEWAY, Collections.singletonList(DEFAULT_VHOST)));
    }

    @Bean
    ControlPlaneClient controlPlaneClient(
            @Value("${apigateway.control-plane.url:http://control-plane:8080}") String controlPlaneUrl,
            MicroserviceRestClient microserviceRestClient) {
        return new SpringControlPlaneClient(controlPlaneUrl, microserviceRestClient);
    }

    @Bean
    RouteRetryManager routePostManager(Scheduler rxScheduler, RoutesRegistrationDelayProvider routesRegistrationDelayProvider) {
        return new RouteRetryManager(rxScheduler, routesRegistrationDelayProvider);
    }

    @Bean
    RouteTransformer routeTransformer() {
        return new RouteTransformer(MICROSERVICE_TEST_NAME);
    }

    @Bean
    RegistrationRequestFactory registrationRequestFactory() {
        return new RegistrationRequestFactory(MICROSERVICE_URL, MICROSERVICE_TEST_NAME, "v1", "default");
    }

    @Bean
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor(
            ControlPlaneClient controlPlaneClient,
            RouteRetryManager routeRetryManager,
            RouteTransformer routeTransformer,
            RegistrationRequestFactory registrationRequestFactory) {
        return new RoutesRestRegistrationProcessor(controlPlaneClient,
                routeRetryManager,
                routeTransformer,
                registrationRequestFactory,
                true,
                MICROSERVICE_TEST_NAME,
                MICROSERVICE_TEST_NAME + ":" + PORT + CONTEXT_PATH);
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

    @Bean
    SecurityProperties getSecurityProperties() {
        return new SecurityProperties();
    }

    @Bean
    MethodValidationPostProcessor getMethodValidationPostProcessor() {
        return new MethodValidationPostProcessor();
    }
}
