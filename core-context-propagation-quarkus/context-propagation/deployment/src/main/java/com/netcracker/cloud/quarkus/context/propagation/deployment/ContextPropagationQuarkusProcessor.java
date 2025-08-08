package org.qubership.cloud.quarkus.context.propagation.deployment;

import org.qubership.cloud.context.propagation.quarkus.runtime.configuration.QuarkusContextProvidersRecorder;
import org.qubership.cloud.context.propagation.quarkus.runtime.filter.QuarkusContextProviderResponseFilter;
import org.qubership.cloud.context.propagation.quarkus.runtime.filter.QuarkusPostAuthnContextProviderFilter;
import org.qubership.cloud.context.propagation.quarkus.runtime.interceptor.QuarkusRestClientInterceptor;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.Capabilities;
import io.quarkus.deployment.Capability;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.AdditionalIndexedClassesBuildItem;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.resteasy.common.spi.ResteasyJaxrsProviderBuildItem;
import io.quarkus.resteasy.reactive.spi.*;
import io.quarkus.vertx.http.deployment.FilterBuildItem;

import static org.qubership.cloud.context.propagation.quarkus.runtime.filter.Priorities.CORE_CONTEXT_PROPAGATION_INCOMING_REQUEST;
import static org.qubership.cloud.context.propagation.quarkus.runtime.filter.Priorities.CORE_CONTEXT_PROPAGATION_OUTGOING_RESPONSE;
import static io.quarkus.deployment.annotations.ExecutionTime.STATIC_INIT;

public class ContextPropagationQuarkusProcessor {

    private static final String FEATURE = "nc-context-propagation-quarkus";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void registerServerFilters(BuildProducer<CustomContainerRequestFilterBuildItem> customRequestFilters,
                               BuildProducer<CustomContainerResponseFilterBuildItem> customResponseFilters,
                               BuildProducer<ContainerRequestFilterBuildItem> requestFilters,
                               BuildProducer<ContainerResponseFilterBuildItem> responseFilters,
                               BuildProducer<ResteasyJaxrsProviderBuildItem> providers,
                               BuildProducer<DynamicFeatureBuildItem> dynamicFeatures,
                               Capabilities capabilities) {
        providers.produce(new ResteasyJaxrsProviderBuildItem(QuarkusPostAuthnContextProviderFilter.class.getName()));
        providers.produce(new ResteasyJaxrsProviderBuildItem(QuarkusContextProviderResponseFilter.class.getName()));
        if (capabilities.isPresent(Capability.RESTEASY_REACTIVE)) {
            requestFilters.produce(new ContainerRequestFilterBuildItem.Builder(
                    QuarkusPostAuthnContextProviderFilter.class.getName())
                    .setPriority(CORE_CONTEXT_PROPAGATION_INCOMING_REQUEST)
                    .build()
            );
            responseFilters.produce(new ContainerResponseFilterBuildItem.Builder(
                    QuarkusContextProviderResponseFilter.class.getName())
                    .setPriority(CORE_CONTEXT_PROPAGATION_OUTGOING_RESPONSE)
                    .build()
            );
        }
    }

    @BuildStep
    void registerClientFilters(BuildProducer<AdditionalBeanBuildItem> additionalBeans,
                               BuildProducer<AdditionalIndexedClassesBuildItem> additionalIndexedClassesBuildItem) {
        additionalBeans.produce(AdditionalBeanBuildItem.unremovableOf(QuarkusRestClientInterceptor.class));
        additionalIndexedClassesBuildItem.produce(new AdditionalIndexedClassesBuildItem(QuarkusRestClientInterceptor.class.getName()));
    }

    @BuildStep
    @Record(STATIC_INIT)
    void setupAuthenticationMechanisms(
            QuarkusContextProvidersRecorder recorder,
            BuildProducer<FilterBuildItem> filterBuildItemBuildProducer) {
        filterBuildItemBuildProducer
                .produce(new FilterBuildItem(
                        recorder.preAuthnContextProviderHandler(),
                        FilterBuildItem.AUTHENTICATION + 1));
    }
}

