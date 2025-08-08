package org.qubership.maas.declarative.kafka.client.impl.topic;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.MaasKafkaTopicService;
import org.qubership.maas.declarative.kafka.client.api.exception.MaasKafkaException;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.impl.topic.provider.api.MaasKafkaTopicServiceProvider;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Comparator;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class MaasKafkaAggregationTopicService implements MaasKafkaTopicService {

    private static final Logger LOGGER = LoggerFactory.getLogger(MaasKafkaAggregationTopicService.class);

    private final List<MaasKafkaTopicServiceProvider> maasKafkaTopicServiceProviders;

    public MaasKafkaAggregationTopicService(List<MaasKafkaTopicServiceProvider> maasKafkaTopicServiceProviders) {
        this.maasKafkaTopicServiceProviders = sortTopicServices(maasKafkaTopicServiceProviders);
    }

    @Override
    public TopicAddress getTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        return callTopicServiceMethod(maasKafkaTopicService -> maasKafkaTopicService.getTopicAddressByDefinition(clientDefinition));
    }

    @Override
    public TopicAddress getTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return callTopicServiceMethod(maasKafkaTopicService -> maasKafkaTopicService.getTopicAddressByDefinitionAndTenantId(clientDefinition, tenantId));
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinition(MaasKafkaCommonClientDefinition clientDefinition) {
        return callTopicServiceMethod(maasKafkaTopicService -> maasKafkaTopicService.getOrCreateTopicAddressByDefinition(clientDefinition));
    }

    @Override
    public TopicAddress getOrCreateTopicAddressByDefinitionAndTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return callTopicServiceMethod(maasKafkaTopicService -> maasKafkaTopicService.getOrCreateTopicAddressByDefinitionAndTenantId(clientDefinition, tenantId));
    }

    @Override
    public String getFullyQualifiedTopicAddressName(MaasKafkaCommonClientDefinition clientDefinition) {
        return callTopicServiceMethod(maasKafkaTopicService -> maasKafkaTopicService.getFullyQualifiedTopicAddressName(clientDefinition));
    }

    @Override
    public String getFullyQualifiedTopicAddressNameByTenantId(MaasKafkaCommonClientDefinition clientDefinition, String tenantId) {
        return callTopicServiceMethod(maasKafkaTopicService -> maasKafkaTopicService.getFullyQualifiedTopicAddressNameByTenantId(clientDefinition, tenantId));
    }

    // Utils methods
    <T> T callTopicServiceMethod(Function<MaasKafkaTopicService, T> methodExecutor) {
        for (MaasKafkaTopicServiceProvider provider : maasKafkaTopicServiceProviders) {
            try {
                MaasKafkaTopicService service = provider.provide();
                if (service != null) {
                    return methodExecutor.apply(service);
                }
            } catch (Throwable ex) {
                LOGGER.error("Unexpected error occurred", ex);
            }
        }

        throw new MaasKafkaException("There is no valid MaasKafkaTopicService provider");
    }

    List<MaasKafkaTopicServiceProvider> sortTopicServices(List<MaasKafkaTopicServiceProvider> topicServiceProviders) {
        return topicServiceProviders.stream().sorted(Comparator.comparing(MaasKafkaTopicServiceProvider::order)).collect(Collectors.toList());
    }
}
