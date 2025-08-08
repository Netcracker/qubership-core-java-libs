package com.netcracker.cloud.maas.client.impl.kafka.protocolextractors;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.cloud.maas.client.api.kafka.TopicUserCredentials;

import java.util.Map;
import java.util.Optional;


public class ScramPropertiesExtractor  extends ConnectionPropertiesExtractorAbstract {
	@Override
	protected String addressProtocolName() {
		return "SASL_PLAINTEXT";
	}

	@Override
	protected Optional<Map<String, String>> extractCredentials(TopicAddress info) {
		return info.getCredentials("SCRAM").map(ScramPropertiesExtractor::formatScramProps);
	}

	static Map<String, String> formatScramProps(TopicUserCredentials credentials) {
		return Map.of(
				SASL_MECHANISM_CONFIG, "SCRAM-SHA-512",
				SASL_JAAS_CONFIG, String.format("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"%s\" password=\"%s\";",
						credentials.getUsername(),
						credentials.getPassword()
				)
		);
	}

	@Override
	public int getExtractorPriority() {
		return 10;
	}
}
