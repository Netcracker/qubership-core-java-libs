package org.qubership.cloud.maas.client.impl.kafka.protocolextractors;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.TopicUserCredentials;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class PlaintextPropertiesExtractor extends ConnectionPropertiesExtractorAbstract {
	@Override
	protected String addressProtocolName() {
		return "PLAINTEXT";
	}

	@Override
	protected Optional<Map<String, String>> extractCredentials(TopicAddress info) {
		return info.getCredentials("plain").map(PlaintextPropertiesExtractor::formatPlainProps)
				.or(() -> Optional.of(Map.of()));
	}

	static Map<String, String> formatPlainProps(TopicUserCredentials credentials) {
		return Map.of(
				SASL_MECHANISM_CONFIG, "PLAIN",
				SASL_JAAS_CONFIG, String.format("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"%s\" password=\"%s\";",
						credentials.getUsername(),
						credentials.getPassword()
				)
		);
	}


	@Override
	public int getExtractorPriority() {
		return 0;
	}
}
