package org.qubership.maas.declarative.kafka.quarkus.client.config;

import org.qubership.cloud.maas.bluegreen.kafka.Record;
import org.qubership.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import org.qubership.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl.NoopFilter;
import org.qubership.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import org.qubership.maas.declarative.kafka.client.impl.client.notification.impl.MaasKafkaClientStateChangeNotificationServiceImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.impl.DefaultInternalMaasTopicCredentialsExtractorImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.impl.InternalMaasTopicCredExtractorAggregatorImpl;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;
import org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl.DefaultInternalMaasTopicCredentialsExtractorProviderImpl;
import org.qubership.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import org.qubership.maas.declarative.kafka.client.impl.definition.impl.MaasKafkaClientDefinitionServiceImpl;
import org.qubership.maas.declarative.kafka.quarkus.client.impl.MaasKafkaClientConfigPlatformServiceImpl;
import org.qubership.maas.declarative.kafka.quarkus.client.impl.QuarkusContextPropagationServiceImpl;
import org.qubership.maas.declarative.kafka.quarkus.client.impl.QuarkusKafkaClientCreationServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import io.opentelemetry.api.GlobalOpenTelemetry;
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

import static org.qubership.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter.CONTEXT_PROPAGATION_ORDER;
import static java.lang.Boolean.FALSE;

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
    KafkaClientCreationService defaultKafkaClientCreationService(MeterRegistry meterRegistry) {
        if (props.kafkaMonitoringEnabled) {
            return new QuarkusKafkaClientCreationServiceImpl(meterRegistry, props.tracingEnabled);
        }
        return new QuarkusKafkaClientCreationServiceImpl(null, props.tracingEnabled);
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
    public ConsumerRecordFilter tracingVertexFilter(Vertx vertx) {
        if (FALSE.equals(props.tracingEnabled)) {
            return NoopFilter.INSTANCE;
        }
        return new ConsumerRecordFilter() {
            final KafkaInstrumenterFactory instrumenterFactory = new KafkaInstrumenterFactory(GlobalOpenTelemetry.get(), "maas-declarative-kafka");
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
