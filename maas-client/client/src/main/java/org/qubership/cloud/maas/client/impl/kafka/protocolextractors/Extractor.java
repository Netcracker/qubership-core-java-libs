package org.qubership.cloud.maas.client.impl.kafka.protocolextractors;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.protocolextractors.ConnectionPropertiesExtractor;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.ServiceLoader;
import java.util.stream.Collectors;

public class Extractor {
    private static final List<ConnectionPropertiesExtractor> CONNECTION_PROTOCOL_EXTRACTORS =
            ServiceLoader.load(ConnectionPropertiesExtractor.class)
                    .stream()
                    .map(ServiceLoader.Provider::get)
                    .sorted()
                    .collect(Collectors.toList());

    public static Optional<Map<String, Object>> extract(TopicAddress address) {
        assert(CONNECTION_PROTOCOL_EXTRACTORS.size() > 0);

        return CONNECTION_PROTOCOL_EXTRACTORS.stream()
                .map(e -> e.extract(address))
                .filter(Optional::isPresent)
                .findFirst()
                .orElse(Optional.empty());
    }
}
