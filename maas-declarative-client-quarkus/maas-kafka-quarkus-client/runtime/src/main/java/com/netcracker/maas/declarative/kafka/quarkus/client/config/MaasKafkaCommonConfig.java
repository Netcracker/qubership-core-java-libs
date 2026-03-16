package com.netcracker.maas.declarative.kafka.quarkus.client.config;

import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import com.netcracker.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.NoopFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.impl.MaasKafkaClientStateChangeNotificationServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.DefaultInternalMaasTopicCredentialsExtractorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.InternalMaasTopicCredExtractorAggregatorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl.DefaultInternalMaasTopicCredentialsExtractorProviderImpl;
import com.netcracker.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import com.netcracker.maas.declarative.kafka.client.impl.definition.impl.MaasKafkaClientDefinitionServiceImpl;
import com.netcracker.maas.declarative.kafka.quarkus.client.impl.MaasKafkaClientConfigPlatformServiceImpl;
import com.netcracker.maas.declarative.kafka.quarkus.client.impl.QuarkusContextPropagationServiceImpl;
import com.netcracker.maas.declarative.kafka.quarkus.client.impl.QuarkusKafkaClientCreationServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.kafka.internal.KafkaInstrumenterFactory;
import io.opentelemetry.instrumentation.kafka.internal.KafkaProcessRequest;
import io.quarkus.arc.DefaultBean;
import io.quarkus.vertx.core.runtime.context.VertxContextSafetyToggle;
import io.vertx.core.Vertx;
import io.vertx.core.impl.ContextInternal;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Instance;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Inject;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import org.eclipse.microprofile.config.Config;

import java.util.stream.Collectors;

import static com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter.CONTEXT_PROPAGATION_ORDER;
import static java.lang.Boolean.FALSE;
import static java.lang.Boolean.TRUE;

@Singleton
public class MaasKafkaCommonConfig {

    @Inject
    MaasKafkaProps props;

    // cred extractor
    @Singleton
    @Produces
    InternalMaasTopicCredentialsExtractor topicCredentialsExtractor(
            Instance<InternalMaasCredExtractorProvider> credExtractorProviders
    ) {
        return new InternalMaasTopicCredExtractorAggregatorImpl(credExtractorProviders.stream().collect(Collectors.toList()));
    }

    @Singleton
    @Produces
    InternalMaasCredExtractorProvider defaultExtractorProvider() {
        return new DefaultInternalMaasTopicCredentialsExtractorProviderImpl(
                new DefaultInternalMaasTopicCredentialsExtractorImpl()
        );
    }

    // kafka client creation
    @Singleton
    @Produces
    @DefaultBean
    KafkaClientCreationService defaultKafkaClientCreationService(MeterRegistry meterRegistry, Instance<OpenTelemetry> openTelemetry) {
        var metrics = TRUE.equals(props.kafkaMonitoringEnabled) ? meterRegistry : null;
        var telemetry = TRUE.equals(props.tracingEnabled) && openTelemetry.isResolvable() ? openTelemetry.get() : null;
        return new QuarkusKafkaClientCreationServiceImpl(metrics, telemetry);
    }

    @Singleton
    @Produces
    @DefaultBean
    MeterRegistry meterRegistry() {
        return new SimpleMeterRegistry();
    }

    // definition
    @Singleton
    @Produces
    MaasKafkaClientDefinitionService maasKafkaClientDefinitionService(Config config) {
        return new MaasKafkaClientDefinitionServiceImpl(new MaasKafkaClientConfigPlatformServiceImpl(config));
    }

    // context propagation
    @Singleton
    @Produces
    @DefaultBean
    ContextPropagationService defaultContextPropagationService() {
        return new QuarkusContextPropagationServiceImpl();
    }

    // client change state notification
    @Singleton
    @Produces
    MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService() {
        return new MaasKafkaClientStateChangeNotificationServiceImpl();
    }

    @Singleton
    @Produces
    @DefaultBean
    public ContextPropagationFilter contextPropagationFilter(ContextPropagationService contextPropagationService) {
        return new ContextPropagationFilter(contextPropagationService);
    }

    @Produces
    @ApplicationScoped
    @Named("maasTracingVertexFilter")
    @DefaultBean
    public ConsumerRecordFilter tracingVertexFilter(Vertx vertx, Instance<OpenTelemetry> openTelemetryInstance) {
        if (FALSE.equals(props.tracingEnabled) || !openTelemetryInstance.isResolvable()) {
            return NoopFilter.INSTANCE;
        }
        return new ConsumerRecordFilter() {
            final KafkaInstrumenterFactory instrumenterFactory = new KafkaInstrumenterFactory(openTelemetryInstance.get(), "maas-declarative-kafka");
            final Instrumenter<KafkaProcessRequest, Void> consumerProcessInstrumenter = instrumenterFactory.createConsumerProcessInstrumenter();

            @Override
            public void doFilter(Record<?, ?> rec, Chain<Record<?, ?>> next) {

                ContextInternal currentContext = (ContextInternal) vertx.getOrCreateContext();
                ContextInternal duplicatedContext = currentContext.duplicate();
                VertxContextSafetyToggle.setContextSafe(duplicatedContext, true);
                ContextInternal contextInternal = duplicatedContext.beginDispatch();
                try {
                    KafkaProcessRequest kafkaProcessRequest = KafkaProcessRequest.create(rec.getConsumerRecord(), null);
                    Context current = consumerProcessInstrumenter.start(Context.current(), kafkaProcessRequest);
                    try (Scope ignored = current.makeCurrent()) {
                        next.doFilter(rec);
                    } finally {
                        consumerProcessInstrumenter.end(current, kafkaProcessRequest, null, null);
                    }
                } finally {
                    duplicatedContext.endDispatch(contextInternal);
                }
            }

            @Override
            public int order() {
                return CONTEXT_PROPAGATION_ORDER + 1;
            }
        };
    }
}
