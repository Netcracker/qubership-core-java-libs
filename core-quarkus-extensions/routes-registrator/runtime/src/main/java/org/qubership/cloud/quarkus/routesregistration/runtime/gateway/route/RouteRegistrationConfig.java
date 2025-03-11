package org.qubership.cloud.quarkus.routesregistration.runtime.gateway.route;

import org.qubership.cloud.quarkus.security.auth.M2MManager;
import org.qubership.cloud.routesregistration.common.gateway.route.*;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import org.qubership.cloud.security.core.auth.Token;
import io.quarkus.arc.Unremovable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.Optional;

@ApplicationScoped
public class RouteRegistrationConfig {
    private final static String DEFAULT_CONTROL_PLANE_ADDRESS = "http://control-plane:8080";

    public static final String CONTROL_PLANE_HTTP_CLIENT = "controlPlaneHttpClient";

    private String microserviceName;

    private String cloudNamespace;

    private String cloudServiceName;

    private Optional<String> controlPlaneUrl;

    private Boolean postRoutesAppnameDisabled;

    private String microservicePort;

    private Boolean postRoutesEnabled;

    private Optional<String> deploymentVersion;

    public RouteRegistrationConfig(@ConfigProperty(name = "cloud.microservice.name") String microserviceName,
                                   @ConfigProperty(name = "cloud.microservice.namespace") String cloudNamespace,
                                   @ConfigProperty(name = "apigateway.control-plane.url") Optional<String> controlPlaneUrl,
                                   @ConfigProperty(name = "apigateway.routes.registration.appname.disabled", defaultValue = "false") Boolean postRoutesAppnameDisabled,
                                   @ConfigProperty(name = "quarkus.http.port", defaultValue = "8080") String microservicePort,
                                   @ConfigProperty(name = "apigateway.routes.registration.enabled", defaultValue = "true") Boolean postRoutesEnabled,
                                   @ConfigProperty(name = "cloud.microservice.bg_version") Optional<String> deploymentVersion) {
        this.microserviceName = microserviceName;
        this.cloudNamespace = cloudNamespace;
        this.controlPlaneUrl = controlPlaneUrl;
        this.postRoutesAppnameDisabled = postRoutesAppnameDisabled;
        this.microservicePort = microservicePort;
        this.postRoutesEnabled = postRoutesEnabled;

        this.cloudServiceName = microserviceName;
        this.deploymentVersion = deploymentVersion;
        deploymentVersion.ifPresent(s -> this.cloudServiceName += "-" + s);
    }

    /**
     * RouteTransformer is a bean that performs {@link RouteEntry} transformation based on {@link RouteType}
     * before sending routes to control-plane.
     *
     * @return {@code RouteTransformer} bean.
     */
    @Produces
    RouteTransformer routeTransformer() {
        return new RouteTransformer(microserviceName);
    }


    @Produces
    ControlPlaneClient controlPlaneClient(@Named(CONTROL_PLANE_HTTP_CLIENT) OkHttpClient controlPlaneHttpClient) {
        return new QuarkusControlPlaneClient(controlPlaneUrl.orElse(DEFAULT_CONTROL_PLANE_ADDRESS), controlPlaneHttpClient);
    }

    @Produces
    @Named(CONTROL_PLANE_HTTP_CLIENT)
    OkHttpClient controlPlaneHttpClient() {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Token token = M2MManager.getInstance().getToken();
                    Request original = chain.request();
                    Request request = original.newBuilder()
                            .addHeader("Authorization", token.getTokenType() + " " + token.getTokenValue())
                            .build();
                    return chain.proceed(request);
                })
                .retryOnConnectionFailure(true)
                .build();
    }

    @Produces
    RouteRetryManager routePostManager(Scheduler rxScheduler, RoutesRegistrationDelayProvider routesRegistrationDelayProvider) {
        return new RouteRetryManager(rxScheduler, routesRegistrationDelayProvider);
    }

    @Produces
    Scheduler rxScheduler() {
        return Schedulers.computation();
    }

    @Produces
    RoutesRegistrationDelayProvider routesRegistrationDelayProvider() {
        RoutesRegistrationDelayProvider routesRegistrationDelayProvider = new RoutesRegistrationDelayProvider();
        routesRegistrationDelayProvider.setProperties();
        return routesRegistrationDelayProvider;
    }

    @Produces
    RegistrationRequestFactory registrationRequestFactory() {
        String microserviceInternalURL = Utils.formatMicroserviceInternalURL(
                cloudServiceName,
                microserviceName,
                getPort(),
                "/",
                postRoutesAppnameDisabled
        );
        return new RegistrationRequestFactory(microserviceInternalURL, microserviceName, deploymentVersion.orElse(null), cloudNamespace);
    }

    @Produces
    @Unremovable
    RoutesRestRegistrationProcessor routesRestRegistrationProcessor(ControlPlaneClient controlPlaneClient,
                                                                    RouteRetryManager retryManager,
                                                                    RouteTransformer routeTransformer,
                                                                    RegistrationRequestFactory registrationRequestFactory) {
        return new RoutesRestRegistrationProcessor(
                controlPlaneClient,
                retryManager,
                routeTransformer,
                registrationRequestFactory,
                postRoutesEnabled,
                microserviceName,
                Utils.formatMicroserviceInternalURL(
                        cloudServiceName, microserviceName, getPort(), "/", postRoutesAppnameDisabled)
        );
    }

    private String getPort() {
        return microservicePort;
    }
}