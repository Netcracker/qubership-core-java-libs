package com.netcracker.maas.declarative.kafka.spring.client.config;

import com.netcracker.cloud.maas.bluegreen.kafka.Record;
import com.netcracker.maas.declarative.kafka.client.api.context.propagation.ContextPropagationService;
import com.netcracker.maas.declarative.kafka.client.api.filter.ConsumerRecordFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.Chain;
import com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter;
import com.netcracker.maas.declarative.kafka.client.impl.client.creator.KafkaClientCreationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.impl.MaasKafkaClientStateChangeNotificationServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.context.propagation.DefaultContextPropagationServiceImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.api.InternalMaasTopicCredentialsExtractor;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.DefaultInternalMaasTopicCredentialsExtractorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.impl.InternalMaasTopicCredExtractorAggregatorImpl;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.api.InternalMaasCredExtractorProvider;
import com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor.provider.impl.DefaultInternalMaasTopicCredentialsExtractorProviderImpl;
import com.netcracker.maas.declarative.kafka.client.impl.definition.api.MaasKafkaClientDefinitionService;
import com.netcracker.maas.declarative.kafka.client.impl.definition.impl.MaasKafkaClientDefinitionServiceImpl;
import com.netcracker.maas.declarative.kafka.spring.client.impl.MaasKafkaClientConfigPlatformServiceImpl;
import com.netcracker.maas.declarative.kafka.spring.client.impl.SpringKafkaClientCreationServiceImpl;
import io.micrometer.core.instrument.MeterRegistry;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.Scope;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.opentelemetry.extension.trace.propagation.B3Propagator;
import io.opentelemetry.instrumentation.api.instrumenter.Instrumenter;
import io.opentelemetry.instrumentation.kafka.internal.KafkaInstrumenterFactory;
import io.opentelemetry.instrumentation.kafka.internal.KafkaProcessRequest;
import org.springframework.beans.factory.BeanFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

import static com.netcracker.maas.declarative.kafka.client.impl.client.consumer.filter.impl.ContextPropagationFilter.CONTEXT_PROPAGATION_ORDER;

@Configuration
public class MaasKafkaCommonConfig {

    // cred extractor
    @Bean
    InternalMaasTopicCredentialsExtractor topicCredentialsExtractor(
            List<InternalMaasCredExtractorProvider> credExtractorProviders
    ) {
        return new InternalMaasTopicCredExtractorAggregatorImpl(credExtractorProviders);
    }

    @Bean
    InternalMaasCredExtractorProvider defaultExtractorProvider() {
        return new DefaultInternalMaasTopicCredentialsExtractorProviderImpl(
                new DefaultInternalMaasTopicCredentialsExtractorImpl()
        );
    }

    // kafka client creation
    @Bean
    @ConditionalOnMissingBean(KafkaClientCreationService.class)
    KafkaClientCreationService defaultKafkaClientCreationService(@Value("${management.tracing.enabled:true}") boolean tracingEnabled,
                                                                 @Autowired BeanFactory beanFactory,
                                                                 @Value("${maas.kafka.monitoring.enabled:true}") boolean monitoringEnabled,
                                                                 @Autowired(required = false) MeterRegistry meterRegistry,
                                                                 @Autowired(required = false) OpenTelemetry openTelemetry

    ) {
        return new SpringKafkaClientCreationServiceImpl(tracingEnabled, beanFactory, meterRegistry, openTelemetry);
    }

    // definition
    @Bean
    MaasKafkaClientDefinitionService maasKafkaClientDefinitionService(MaasKafkaClientConfigKeeper configKeeper) {
        return new MaasKafkaClientDefinitionServiceImpl(new MaasKafkaClientConfigPlatformServiceImpl(configKeeper));
    }

    // context propagation
    @Bean
    ContextPropagationService defaultContextPropagationService() {
        return new DefaultContextPropagationServiceImpl();
    }

    // client change state notification
    @Bean
    MaasKafkaClientStateChangeNotificationService maasKafkaClientStateChangeNotificationService() {
        return new MaasKafkaClientStateChangeNotificationServiceImpl();
    }

    @Bean
    public ContextPropagationFilter contextPropagationFilter(ContextPropagationService contextPropagationService) {
        return new ContextPropagationFilter(contextPropagationService);
    }

    @Bean("maasTracingFilter")
    @ConditionalOnProperty(name = "management.tracing.enabled", havingValue = "true", matchIfMissing = true)
    public ConsumerRecordFilter maasTracingFilter(OpenTelemetry openTelemetry) {
        return new ConsumerRecordFilter() {
            final KafkaInstrumenterFactory instrumenterFactory = new KafkaInstrumenterFactory(openTelemetry, "maas-declarative-kafka");
            final Instrumenter<KafkaProcessRequest, Void> consumerProcessInstrumenter = instrumenterFactory.createConsumerProcessInstrumenter();

            @Override
            public void doFilter(Record<?, ?> rec, Chain<Record<?, ?>> next) {
                KafkaProcessRequest kafkaProcessRequest = KafkaProcessRequest.create(rec.getConsumerRecord(), null);
                Context current = consumerProcessInstrumenter.start(Context.current(), kafkaProcessRequest);
                try (Scope ignored = current.makeCurrent()) {
                    next.doFilter(rec);
                } finally {
                    consumerProcessInstrumenter.end(current, kafkaProcessRequest, null, null);
                }
            }

            @Override
            public int order() {
                return CONTEXT_PROPAGATION_ORDER + 1;
            }
        };
    }

    @Bean
    @ConditionalOnMissingBean
    public ContextPropagators customPropagators() {
        return ContextPropagators.create(TextMapPropagator.composite(B3Propagator.injectingMultiHeaders()));
    }
}
