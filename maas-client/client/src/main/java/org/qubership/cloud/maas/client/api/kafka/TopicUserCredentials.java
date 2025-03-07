package org.qubership.cloud.maas.client.api.kafka;

public interface TopicUserCredentials {
    String getUsername();
    String getPassword();
    String getClientKey();
    String getClientCert();
}
