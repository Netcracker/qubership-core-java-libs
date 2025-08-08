package com.netcracker.maas.declarative.kafka.client.impl.common.cred.extractor;

import com.netcracker.cloud.maas.client.api.kafka.TopicAddress;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import com.netcracker.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SslMaasCredExtractorTest extends AbstractCredExtractorTest {

    @Test
    void testSslMaasCredExtractor() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName("/org/qubership/maas/declarative/kafka/client/impl/common/cred/extractor/SSL.json")// Path in resource folder
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
        assertThat(extractedCreds.get("security.protocol")).isEqualTo("SSL");
        assertThat(extractedCreds.get("ssl.truststore.type")).isEqualTo("PEM");
        assertThat(extractedCreds.get("ssl.truststore.certificates"))
                .isEqualTo("-----BEGIN CERTIFICATE----- MIID7jCCAtagAwIBAgIRAKorRWLpa+wpB5+lf7XixDcwDQ" +
                        "YJKoZIhvcNAQELBQAwgYkxFTATBgNVBAYTDEFua2gtTW9ycG9yazEaMBgGA1UEChMRVW5zZWVuIFV" +
                        "uaXZlcnNpdHkxEDAOBgNVBAsTB0xpYnJhcnkxQjBABgNVBAMTOUxhbmRvb3AncyBGYXN0IERhdGEg" +
                        "RGV2IFNlbGYgU2lnbmVkIENlcnRpZmljYXRlIEF1dGhvcml0eTAeFw0yMDEwMDIxMDI0MTNaFw0yM" +
                        "TEwMDIxNjI0MTNaMIGJMRUwEwYDVQQGEwxBbmtoLU1vcnBvcmsxGjAYBgNVBAoTEVVuc2VlbiBVbm" +
                        "l2ZXJzaXR5MRAwDgYDVQQLEwdMaWJyYXJ5MUIwQAYDVQQDEzlMYW5kb29wJ3MgRmFzdCBEYXRhIER" +
                        "ldiBTZWxmIFNpZ25lZCBDZXJ0aWZpY2F0ZSBBdXRob3JpdHkwggEiMA0GCSqGSIb3DQEBAQUAA4IB" +
                        "DwAwggEKAoIBAQCdkG7znYXF2TMGwnG+BNd13MC4htzrvltbYxs88twruFuUZ6mKjcNfbZkShenwc" +
                        "hK/YHfTVhnVAJjMC/dsKLhB7GbIJ+PfM/weBBPBcqaLL4IXkJvYmNyY3fxhrIzx6cTgvoxR2q22r2" +
                        "hM546egSaafgfsOY/1rZqppK/Tuc0XFTuMnvOidW1f1FXDjJ9twc6rTVTimAYGEtNEFcVscyOyjo5" +
                        "9tXoygsH/fPc7oHPzgaSdSZpSTpe6Y2vP5YA0vfu6Nv1m5N3x+jF9oQIPlzMILJot5cTbr4pdT1ot" +
                        "q71MFyHPUIVOQT1yYT8CTJe4PmIx8iePucY8nAP3ymHJWfS1AgMBAAGjTzBNMA4GA1UdDwEB/wQEA" +
                        "wICpDAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDwYDVR0TAQH/BAUwAwEB/zALBgNVHR" +
                        "EEBDACggAwDQYJKoZIhvcNAQELBQADggEBAJZDDxU/ktUizIklBQddv1EviNq1awIDWO2rx6tKqQ7" +
                        "o6Dp/DzD5SKU7JOUo0XC/XkczINDBh8q2O+rDHFsO35yNgO+OJBn0q7YkOu37Beh9ofG89q7ilCzT" +
                        "qqRrXc6HPzDBfbDvM2F223qoNgzf4fSM+9kNoiib2IaTmDUp9LH9IoTmUPwCbwh2XnWd3fxBIxUjh" +
                        "EaFAfphie/jp6BPF1JouVdMYFyxvKwfYHTQlDm+ufJuFySBx+fI/qV1c612MyTzEHJIgBTrRvC08Q" +
                        "FeWKuwGgYCVDf9tCSD0ThzJE6A/NOO6U0opyOKjSpX3RQT/qViZMEo0DV3RsYgBUL6038= -----END CERTIFICATE-----");
    }
}
