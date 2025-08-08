package org.qubership.cloud.maas.client.impl.kafka.protocolextractors;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.maas.client.impl.dto.kafka.v1.TopicInfo;
import org.qubership.cloud.maas.client.impl.kafka.TopicAddressImpl;
import lombok.SneakyThrows;
import org.junit.jupiter.api.Test;

import static org.qubership.cloud.maas.client.impl.kafka.protocolextractors.ConnectionPropertiesExtractorAbstract.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class ExtractorsTest {
	@Test
	public void propsExtractor_plaintext_anonymous() {
		var props = loadTopicInfo("/protocol-extractors/PLAINTEXT_ANON.json").formatConnectionProperties().get();
		assertEquals("server1:9092", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("PLAINTEXT", props.get(SECURITY_PROTOCOL_CONFIG));
	}

	@Test
	public void propsExtractor_plaintext() {
		var topicAddr = loadTopicInfo("/protocol-extractors/PLAINTEXT.json");
		var props = Extractor.extract(topicAddr).get();
		assertEquals("server1:9092,server2:9092", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("PLAINTEXT", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"admin\" password=\"admin\";", props.get(SASL_JAAS_CONFIG));
		assertEquals("PLAIN", props.get(SASL_MECHANISM_CONFIG));
	}

	@Test
	public void propsExtractor_plaintext_with_scram() {
		var props = loadTopicInfo("/protocol-extractors/PLAINTEXT_SCRAM.json").formatConnectionProperties().get();
		assertEquals("server1:9092,server2:9092", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("PLAINTEXT", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"admin\" password=\"admin\";", props.get(SASL_JAAS_CONFIG));
		assertEquals("SCRAM-SHA-512", props.get(SASL_MECHANISM_CONFIG));
	}

	@Test
	public void propsExtractor_sasl_plaintext() {
		var props = loadTopicInfo("/protocol-extractors/SASL_PLAINTEXT.json").formatConnectionProperties().get();
		assertEquals("kafka.cpq-kafka:9092", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("SASL_PLAINTEXT", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"client\" password=\"client\";", props.get(SASL_JAAS_CONFIG));
		assertEquals("SCRAM-SHA-512", props.get(SASL_MECHANISM_CONFIG));
	}

	@Test
	public void propsExtractor_sasl_ssl_scram_client_certs() {
		var props = loadTopicInfo("/protocol-extractors/SASL_SSL_SCRAM_CLIENT_CERTS.json").formatConnectionProperties().get();
		assertEquals("localkafka.kafka-cluster:9094", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("SASL_SSL", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"alice\" password=\"alice-secret\";", props.get(SASL_JAAS_CONFIG));
		assertEquals("SCRAM-SHA-512", props.get(SASL_MECHANISM_CONFIG));
		assertEquals("PEM", props.get(SSL_TRUSTSTORE_TYPE_CONFIG));
		assertTrue(props.get(SSL_TRUSTSTORE_CERTIFICATES_CONFIG).toString().startsWith("-----BEGIN CERTIFICATE----- MIID7jCCAt"));

		// check client certs presence
		assertTrue(props.get(SSL_KEYSTORE_KEY_CONFIG).toString().startsWith("-----BEGIN RSA PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BA"));
		assertTrue(props.get(SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG).toString().startsWith("-----BEGIN CERTIFICATE----- MIID/TCCAuWgAwIBAgIQHO0Kyu"));
	}

	@Test
	public void propsExtractor_sasl_ssl_scram() {
		var props = loadTopicInfo("/protocol-extractors/SASL_SSL_SCRAM.json").formatConnectionProperties().get();
		assertEquals("localkafka.kafka-cluster:9094", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("SASL_SSL", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"alice\" password=\"alice-secret\";", props.get(SASL_JAAS_CONFIG));
		assertEquals("SCRAM-SHA-512", props.get(SASL_MECHANISM_CONFIG));
		assertEquals("PEM", props.get(SSL_TRUSTSTORE_TYPE_CONFIG));
		assertTrue(props.get(SSL_TRUSTSTORE_CERTIFICATES_CONFIG).toString().startsWith("-----BEGIN CERTIFICATE----- MIID7jCCAt"));
	}

	@Test
	public void propsExtractor_sasl_ssl_plain_client_certs() {
		var props = loadTopicInfo("/protocol-extractors/SASL_SSL_PLAIN_CLIENT_CERTS.json").formatConnectionProperties().get();
		assertEquals("localkafka.kafka-cluster:9094", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("SASL_SSL", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"alice\" password=\"alice-secret\";", props.get(SASL_JAAS_CONFIG));
		assertEquals("PLAIN", props.get(SASL_MECHANISM_CONFIG));
		assertEquals("PEM", props.get(SSL_TRUSTSTORE_TYPE_CONFIG));
		assertTrue(props.get(SSL_TRUSTSTORE_CERTIFICATES_CONFIG).toString().startsWith("-----BEGIN CERTIFICATE----- MIID7jCCAt"));

		// check client certs presence
		assertTrue(props.get(SSL_KEYSTORE_KEY_CONFIG).toString().startsWith("-----BEGIN RSA PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BA"));
		assertTrue(props.get(SSL_KEYSTORE_CERTIFICATE_CHAIN_CONFIG).toString().startsWith("-----BEGIN CERTIFICATE----- MIID/TCCAuWgAwIBAgIQHO0Kyu"));
	}

	@Test
	public void propsExtractor_ssl() {
		var props = loadTopicInfo("/protocol-extractors/SSL.json").formatConnectionProperties().get();
		assertEquals("localkafka.kafka-cluster:9094", props.get(BOOTSTRAP_SERVERS_CONFIG));
		assertEquals("SSL", props.get(SECURITY_PROTOCOL_CONFIG));
		assertEquals("PEM", props.get(SSL_TRUSTSTORE_TYPE_CONFIG));
		assertTrue(props.get(SSL_TRUSTSTORE_CERTIFICATES_CONFIG).toString().startsWith("-----BEGIN CERTIFICATE----- MIID7jCCAt"));
	}

	@SneakyThrows
	private TopicAddressImpl loadTopicInfo(String jsonResource) {
		return new TopicAddressImpl(
				new ObjectMapper().readValue(
					getClass().getResourceAsStream(jsonResource),
					TopicInfo.class
			));
	}
}
