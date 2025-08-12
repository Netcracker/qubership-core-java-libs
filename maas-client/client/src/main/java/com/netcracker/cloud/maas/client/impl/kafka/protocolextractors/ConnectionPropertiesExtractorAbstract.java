package com.netcracker.cloud.maas.client.impl.kafka.protocolextractors;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.cloud.maas.client.api.kafka.protocolextractors.ConnectionPropertiesExtractor;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;


@Slf4j
public abstract class ConnectionPropertiesExtractorAbstract implements ConnectionPropertiesExtractor {

	// constant values from kafka-client lib as strings to exclude explicit dependency on kafka-clients jar that
	// will possibly be conflicting with different version of kafka-client library version in end user application
	static final String BOOTSTRAP_SERVERS_CONFIG = "bootstrap.servers";
	static final String SECURITY_PROTOCOL_CONFIG = "security.protocol";
	static final String SASL_MECHANISM_CONFIG = "sasl.mechanism";
	static final String SASL_JAAS_CONFIG = "sasl.jaas.config";
	static final String SSL_TRUSTSTORE_TYPE_CONFIG = "ssl.truststore.type";
	static final String SSL_KEYSTORE_TYPE_CONFIG = "ssl.keystore.type";
	static final String SSL_KEYSTORE_KEY_CONFIG = "ssl.keystore.key";
	static final String SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG = "ssl.keystore.certificate.chain";
	static final String SSL_TRUSTSTORE_CERTIFICATES_CONFIG = "ssl.truststore.certificates";

	@Override
	public Optional<Map<String, Object>> extract(TopicAddress addr) {
		var props = new HashMap<String, Object>();
		var hosts = addr.getBoostrapServers(addressProtocolName());
		if (hosts == null) {
			log.debug("No addresses for `{}' protocol found", addressProtocolName());
			return Optional.empty();
		}

		log.debug("Found addresses for `{}' protocol. Lookup for credential details", addressProtocolName());
		props.put(BOOTSTRAP_SERVERS_CONFIG, String.join(",", hosts));
		props.put(SECURITY_PROTOCOL_CONFIG, addressProtocolName());

		var c = extractCredentials(addr);
		if (c.isEmpty()) {
			log.warn("Found addresses for `{}' protocol, but can't find corresponding credentials in received topic info", addressProtocolName());
			return Optional.empty();
		}

		log.debug("Found credentials for `{}' protocol", addressProtocolName());
		props.putAll(c.get());

		return Optional.of(props);
	}

	protected abstract String addressProtocolName();

	protected abstract Optional<Map<String, String>> extractCredentials(TopicAddress addr);
}

