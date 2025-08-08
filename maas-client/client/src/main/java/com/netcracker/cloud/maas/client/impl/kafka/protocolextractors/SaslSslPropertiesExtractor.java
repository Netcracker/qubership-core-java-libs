package com.netcracker.cloud.maas.client.impl.kafka.protocolextractors;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.cloud.maas.client.api.kafka.TopicUserCredentials;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class SaslSslPropertiesExtractor extends SslPropertiesExtractor {
	final static String BEGIN_RSA_PRIVATE_KEY = "-----BEGIN RSA PRIVATE KEY----- ";
	final static String END_RSA_PRIVATE_KEY = " -----END RSA PRIVATE KEY-----";

	@Override
	protected String addressProtocolName() {
		return "SASL_SSL";
	}

	@Override
	protected Optional<Map<String, String>> extractCredentials(TopicAddress info) {
		return super.extractCredentials(info)
				.map(props -> new HashMap<>(props)) // convert to modifiable map
				.flatMap(props -> {
					props.put(SSL_KEYSTORE_TYPE_CONFIG, PEM);
					return info.getCredentials("sslCert+SCRAM")
							.map(credentials -> {
								props.putAll(ScramPropertiesExtractor.formatScramProps(credentials));
								props.putAll(formatTruststore(credentials));
								return props;
							}).or(() -> info.getCredentials("sslCert+plain")
									.map(credentials -> {
										props.putAll(PlaintextPropertiesExtractor.formatPlainProps(credentials));
										props.putAll(formatTruststore(credentials));
										return props;
									})
							).or(() -> info.getCredentials("SCRAM")
									.map(credentials -> {
										props.putAll(ScramPropertiesExtractor.formatScramProps(credentials));
										return props;
									})
							);
				});
	}

	private Map<String, String> formatTruststore(TopicUserCredentials credentials) {
		return Map.of(
				SSL_KEYSTORE_KEY_CONFIG, BEGIN_RSA_PRIVATE_KEY + credentials.getClientKey() + END_RSA_PRIVATE_KEY,
				SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG, BEGIN_CERTIFICATE + credentials.getClientCert() + END_CERTIFICATE
		);
	}

	@Override
	public int getExtractorPriority() {
		return 30;
	}
}
