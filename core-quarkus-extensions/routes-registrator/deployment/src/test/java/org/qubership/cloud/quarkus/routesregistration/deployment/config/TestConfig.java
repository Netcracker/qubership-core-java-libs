package org.qubership.cloud.quarkus.routesregistration.deployment.config;

import org.qubership.cloud.quarkus.routesregistration.runtime.gateway.route.QuarkusControlPlaneClient;
import org.qubership.cloud.routesregistration.common.gateway.route.*;
import org.qubership.cloud.routesregistration.common.gateway.route.rest.RegistrationRequestFactory;
import org.qubership.cloud.routesregistration.common.gateway.route.transformation.RouteTransformer;
import io.quarkus.arc.Unremovable;
import io.reactivex.Scheduler;
import io.reactivex.schedulers.Schedulers;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Alternative;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Named;
import okhttp3.*;
import org.eclipse.microprofile.config.inject.ConfigProperty;

import java.util.List;
import java.util.Map;

import static org.qubership.cloud.quarkus.routesregistration.runtime.gateway.route.RouteRegistrationConfig.CONTROL_PLANE_HTTP_CLIENT;

@ApplicationScoped
public class TestConfig {
    @ConfigProperty(name = "cloud.microservice.name")
    String microserviceName;

    @ConfigProperty(name = "cloud.microservice.namespace")
    String cloudNamespace;

    @Alternative
    @Priority(1)
    @Produces
    RegistrationRequestFactory registrationRequestFactory() {
        String microserviceInternalURL = Utils.formatMicroserviceInternalURL(
                microserviceName,
                microserviceName,
                "8080",
                "/",
                false
        );
        return new RegistrationRequestFactory(microserviceInternalURL, microserviceName, "v1", cloudNamespace);
    }

    @Alternative
    @Priority(1)
    @Produces
    RouteTransformer routeTransformer() {
        return new RouteTransformer(microserviceName);
    }

    @Alternative
    @Priority(1)
    @Produces
    ControlPlaneClient controlPlaneClient(@Named(CONTROL_PLANE_HTTP_CLIENT) OkHttpClient controlPlaneHttpClient) {
        return new QuarkusControlPlaneClient("http://some-url", controlPlaneHttpClient);
    }

    @Alternative
    @Priority(1)
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
                true,
                microserviceName,
                "http://quarkus-quickstart-test-v1:8080"
        );
    }

    @Produces
    @Priority(1)
    @Alternative
    @Named(CONTROL_PLANE_HTTP_CLIENT)
    OkHttpClient testControlPlaneHttpClient(RequestRecorder requestRecorder) {
        return new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    requestRecorder.record(chain.request());
                    return new Response.Builder()
                            .code(200)
                            .message("OK")
                            .body(ResponseBody.create(MediaType.get("text/plain"), "OK"))
                            .protocol(Protocol.HTTP_1_1)
                            .request(chain.request())
                            .build();
                }).build();
    }

    @Produces
    @Priority(1)
    @Alternative
    RouteRetryManager testRouteRetryManager() {
        return new TestRouteRetryManager(Schedulers.single(), new RoutesRegistrationDelayProvider());
    }

    private static class TestRouteRetryManager extends RouteRetryManager {

        public TestRouteRetryManager(Scheduler rxScheduler, RoutesRegistrationDelayProvider delayProvider) {
            super(rxScheduler, delayProvider);
        }

        @Override
        public void execute(Runnable runnable) {
            runnable.run();
        }

        @Override
        public void execute(Map<Integer, List<Runnable>> priorityPayload) {
            priorityPayload.values().forEach(runnables -> runnables.forEach(Runnable::run));
        }
    }
}
