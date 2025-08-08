package com.netcracker.cloud.maas.client.impl.kafka.protocolextractors;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.Optional;

@Slf4j
public class SslPropertiesExtractor extends ConnectionPropertiesExtractorAbstract {
	final static String BEGIN_CERTIFICATE = "-----BEGIN CERTIFICATE----- ";
	final static String END_CERTIFICATE = " -----END CERTIFICATE-----";
	final static String PEM = "PEM";

	@Override
	protected String addressProtocolName() {
		return "SSL";
	}

	@Override
	protected Optional<Map<String, String>> extractCredentials(TopicAddress addr) {
		if (addr.getCACert() != null && addr.getCACert().trim().length() > 0) {
			return Optional.of(
					Map.of(
							SSL_TRUSTSTORE_TYPE_CONFIG, PEM,
							SSL_TRUSTSTORE_CERTIFICATES_CONFIG, BEGIN_CERTIFICATE + addr.getCACert() + END_CERTIFICATE
					)
			);
		} else {
			log.error("caCert value is empty for SSL protocol");
			return Optional.empty();
		}
	}

	@Override
	public int getExtractorPriority() {
		return 20;
	}
}
