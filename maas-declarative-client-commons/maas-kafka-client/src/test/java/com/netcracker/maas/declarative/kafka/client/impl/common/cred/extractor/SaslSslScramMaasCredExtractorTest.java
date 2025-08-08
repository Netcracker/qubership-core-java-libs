package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SaslSslScramMaasCredExtractorTest extends AbstractCredExtractorTest {

    @Test
    void testSaslSslScramMaasCredExtractor() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName("/org/qubership/maas/declarative/kafka/client/impl/common/cred/extractor/SASL_SSL_PLAIN_SCRAM.json")// Path in resource folder
                .setNamespace("test")
                .setManagedBy(ManagedBy.SELF)
                .build();

        MaasKafkaCommonClientDefinition clientDefinition = MaasKafkaProducerDefinition.builder()
                .setTopic(topicDefinition)
                .setTenant(false)
                .build();

        TopicAddress topic = maasKafkaTopicService.getTopicAddressByDefinition(clientDefinition);


        Map<String, Object> extractedCreds = credentialsExtractor.extract(topic);


        assertThat(extractedCreds.get("bootstrap.servers")).isEqualTo("localkafka.kafka-cluster:9094");
        assertThat(extractedCreds.get("security.protocol")).isEqualTo("SASL_SSL");
        assertThat(extractedCreds.get("ssl.truststore.type")).isEqualTo("PEM");
        assertThat(extractedCreds.get("sasl.mechanism")).isEqualTo("SCRAM-SHA-512");
        assertThat(extractedCreds.get("sasl.jaas.config"))
                .isEqualTo("org.apache.kafka.common.security.scram.ScramLoginModule required username=\"alice\" password=\"alice-secret\";");
        assertThat(extractedCreds.get("ssl.truststore.certificates"))
                .isEqualTo("-----BEGIN CERTIFICATE----- MIID7jCCAtagAwIBAgIRAKorRWLpa+wpB5+lf7XixDcwDQYJKoZIhvc" +
                        "NAQELBQAwgYkxFTATBgNVBAYTDEFua2gtTW9ycG9yazEaMBgGA1UEChMRVW5zZWVuIFVuaXZlcnNpdHkxEDAOB" +
                        "gNVBAsTB0xpYnJhcnkxQjBABgNVBAMTOUxhbmRvb3AncyBGYXN0IERhdGEgRGV2IFNlbGYgU2lnbmVkIENlcnR" +
                        "pZmljYXRlIEF1dGhvcml0eTAeFw0yMDEwMDIxMDI0MTNaFw0yMTEwMDIxNjI0MTNaMIGJMRUwEwYDVQQGEwxBb" +
                        "mtoLU1vcnBvcmsxGjAYBgNVBAoTEVVuc2VlbiBVbml2ZXJzaXR5MRAwDgYDVQQLEwdMaWJyYXJ5MUIwQAYDVQQ" +
                        "DEzlMYW5kb29wJ3MgRmFzdCBEYXRhIERldiBTZWxmIFNpZ25lZCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiM" +
                        "A0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQCdkG7znYXF2TMGwnG+BNd13MC4htzrvltbYxs88twruFuUZ6m" +
                        "KjcNfbZkShenwchK/YHfTVhnVAJjMC/dsKLhB7GbIJ+PfM/weBBPBcqaLL4IXkJvYmNyY3fxhrIzx6cTgvoxR2" +
                        "q22r2hM546egSaafgfsOY/1rZqppK/Tuc0XFTuMnvOidW1f1FXDjJ9twc6rTVTimAYGEtNEFcVscyOyjo59tXo" +
                        "ygsH/fPc7oHPzgaSdSZpSTpe6Y2vP5YA0vfu6Nv1m5N3x+jF9oQIPlzMILJot5cTbr4pdT1otq71MFyHPUIVOQ" +
                        "T1yYT8CTJe4PmIx8iePucY8nAP3ymHJWfS1AgMBAAGjTzBNMA4GA1UdDwEB/wQEAwICpDAdBgNVHSUEFjAUBgg" +
                        "rBgEFBQcDAQYIKwYBBQUHAwIwDwYDVR0TAQH/BAUwAwEB/zALBgNVHREEBDACggAwDQYJKoZIhvcNAQELBQADg" +
                        "gEBAJZDDxU/ktUizIklBQddv1EviNq1awIDWO2rx6tKqQ7o6Dp/DzD5SKU7JOUo0XC/XkczINDBh8q2O+rDHFs" +
                        "O35yNgO+OJBn0q7YkOu37Beh9ofG89q7ilCzTqqRrXc6HPzDBfbDvM2F223qoNgzf4fSM+9kNoiib2IaTmDUp9" +
                        "LH9IoTmUPwCbwh2XnWd3fxBIxUjhEaFAfphie/jp6BPF1JouVdMYFyxvKwfYHTQlDm+ufJuFySBx+fI/qV1c61" +
                        "2MyTzEHJIgBTrRvC08QFeWKuwGgYCVDf9tCSD0ThzJE6A/NOO6U0opyOKjSpX3RQT/qViZMEo0DV3RsYgBUL60" +
                        "38= -----END CERTIFICATE-----");
    }
}
