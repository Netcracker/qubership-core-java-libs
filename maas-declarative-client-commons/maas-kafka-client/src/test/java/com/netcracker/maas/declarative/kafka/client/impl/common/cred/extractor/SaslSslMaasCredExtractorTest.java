package org.qubership.maas.declarative.kafka.client.impl.common.cred.extractor;

import org.qubership.cloud.maas.client.api.kafka.TopicAddress;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaCommonClientDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasKafkaProducerDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.MaasTopicDefinition;
import org.qubership.maas.declarative.kafka.client.api.model.definition.ManagedBy;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SaslSslMaasCredExtractorTest extends AbstractCredExtractorTest {


    @Test
    void testSaslSslScramMaasCredExtractor() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName("/org/qubership/maas/declarative/kafka/client/impl/common/cred/extractor/SASL_SSL_SCRAM.json")// Path in resource folder
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
        assertThat(extractedCreds.get("ssl.keystore.key"))
                .isEqualTo("-----BEGIN RSA PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQD" +
                        "OcPkCmbOb37+FdYGN0AZFkzc64bcNCSa1njvjYnDMiWlA+sQU8giIKit2w5QR1IHQTsKVOzDY4dKiDOzHSnwiM" +
                        "uqZtHuNBqOgT6F2wyDWfUk0uSC8KTgHxiJspwgcSvtGXt8Z+Akk4NsfGySwCg69qG1IFJAwIiL8+NLjt50H2Y+" +
                        "sUyzGlwL2Z5FvxOKfQDftkcTiv4/cJlQp/Q/73emt00GkJ16DUi1J83Qa2Mwq8pPAjOER3UwsJXR6OTDxnyZbu" +
                        "ZEIbwHIeLW++u+X0An8/ViYNyJm5VYmIFnUbFRm6gq0I35cXIBmOp5zyaZPfBUGgDlklsVLGdeJYXCPt6l5AgM" +
                        "BAAECggEAdUY8TzvdipkrBbfhJxyb0350C6EaCe++68G+J8hOaANPpbhPF87/Bcte8ZLdVNux+xIi+/+qGir/K" +
                        "0vIXtZzekIOf87FuTGiVnmmMf9bcj2uDqN0cy+/QrtN5wdkVh5KbImMmAYNu8N490zGJLMx+I/rtNzppE5gLfF" +
                        "0bCRssn3h1ctSooqqjGI1+fu59Ee9kFMvWdlndqPZqthUu39ZhL6a3xfpIUuqfMPEvG9UZiSRNWcrPwZD56sR1" +
                        "DNRDJhRg4WpTpyjcSFGV9SzD69v9/P5mKix46da04ORQGgOyWl/VIUNON69BNUeSNhcWMSPWKpS4SNE+0x6Ib+" +
                        "y99Ot4QKBgQDacHPxTWWIlcV7IIDSDAmF7URfUwD81SoIBwiO309CkwOZxTpUWZNOr6wauFwgZa+ltyT7V3jHP" +
                        "MzqWKo1rzjGO1XZLRT/7xToiZwNXsvF+FyMoADQoJommFOzI2XTHl+Xt58H3DfVQ5vb01QaU7nE/Y5/Gdl0o5B" +
                        "dwJBgzeZQVQKBgQDx8GCcidai4npFiKuUQPaDoLflq9la2T/7/y9sHOaF9f7Jpg35nFakWEB9e7VWs7+AycxzO" +
                        "YXSMjP9QczGpLvKZFv70a8ewhopUGoSbAxXOHYLNlfimSLcThF9edQGkyU2h+whQBJBPr4mMa326fiUIM6NZOx" +
                        "HTIcWM74JgMdIlQKBgQCT13BhbQS/QL776q/Feign4MJZ0g/lD1D3Retg53r5kA7r0DT+SfINW5CcNRMFSk1rV" +
                        "11MT5OLJXd7VK8+zm0anaYo6v4/ik0YZaqc0gAQ6VuaQeDLUyeB368mm1qbXP9N7BVSeJXBXZTrX0iY2Pnufb4" +
                        "oABLRgrJ/AYm2OyUaUQKBgANu59AQ3KzfDeEiuVyXN2hSxYlK0QDLCG9pBaFBH7xS61StOJGMOGaIohGlycL7N" +
                        "gJ72pzf+hgrgjVb064V9FxA5FVEenmzQ2/GplqwkdRDBtnN4Z1jku0RZoojSnD1cZe+gglOwXOPkl80YUWZuF9" +
                        "Rv9ydzASO/3O/NDkqTKmpAoGBAL+QJfrzQ/Q9EiiXmOI3LUK/UeAKQAyf3J4QFRNFKLixlbl3Xa/Fa2y2YlVAb" +
                        "gnYxUcRcsaUmv77MNL5CxwUvC9hEnQAsGcPRjKNx6Qjuy/piuHjgueGLrY+nrgeTN/j+J111V6Db0kg/vU1PlE" +
                        "SKHfT4qvZ7FWRLl5B22qVRR4h -----END RSA PRIVATE KEY-----");
        assertThat(extractedCreds.get("ssl.keystore.certificate.chain"))
                .isEqualTo("-----BEGIN CERTIFICATE----- MIID/TCCAuWgAwIBAgIQHO0KyuduE+XLmgQniMWsRjANBgkqhkiG9w0" +
                        "BAQsFADCBiTEVMBMGA1UEBhMMQW5raC1Nb3Jwb3JrMRowGAYDVQQKExFVbnNlZW4gVW5pdmVyc2l0eTEQMA4GA" +
                        "1UECxMHTGlicmFyeTFCMEAGA1UEAxM5TGFuZG9vcCdzIEZhc3QgRGF0YSBEZXYgU2VsZiBTaWduZWQgQ2VydGl" +
                        "maWNhdGUgQXV0aG9yaXR5MB4XDTIwMTAwMjEwMjQxNVoXDTMwMDkzMDEwMjQxNVowVjEVMBMGA1UEBhMMQW5ra" +
                        "C1Nb3Jwb3JrMRowGAYDVQQKExFVbnNlZW4gVW5pdmVyc2l0eTEQMA4GA1UECxMHTGlicmFyeTEPMA0GA1UEAxM" +
                        "GY2xpZW50MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAznD5Apmzm9+/hXWBjdAGRZM3OuG3DQkmt" +
                        "Z4742JwzIlpQPrEFPIIiCordsOUEdSB0E7ClTsw2OHSogzsx0p8IjLqmbR7jQajoE+hdsMg1n1JNLkgvCk4B8Y" +
                        "ibKcIHEr7Rl7fGfgJJODbHxsksAoOvahtSBSQMCIi/PjS47edB9mPrFMsxpcC9meRb8Tin0A37ZHE4r+P3CZUK" +
                        "f0P+93prdNBpCdeg1ItSfN0GtjMKvKTwIzhEd1MLCV0ejkw8Z8mW7mRCG8ByHi1vvrvl9AJ/P1YmDciZuVWJiB" +
                        "Z1GxUZuoKtCN+XFyAZjqec8mmT3wVBoA5ZJbFSxnXiWFwj7epeQIDAQABo4GSMIGPMA4GA1UdDwEB/wQEAwIFo" +
                        "DAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDAYDVR0TAQH/BAIwADBQBgNVHREESTBHggw3ZDJlNDQ" +
                        "1NzViYmSCBWthZmthghhsb2NhbGthZmthLmthZmthLWNsdXN0ZXKHBH8AAAGHBH8AAAGHBMCoY2SHBKwftoAwD" +
                        "QYJKoZIhvcNAQELBQADggEBAG4N5VRLUbYd39KR/bfVMftfhSKd3MnXgIT6goWL36ShmqD7MKnoRIyVmlSe2Ko" +
                        "0laJiVvFrhzd/W6U8O7J5OMEdKjF3VZPrFTOz/oSOFr6PO7jt3h6Z91Tq2lVkupoilacw0DCirgA1EYIxZsqKs" +
                        "jje941qZxHuJ4AssNox3P2eU5tWo/caSHPG2P3KBmCnC9eVVU9B5iD894DKr1ueFYg3fmDIDcXYu/Dq32oAO80" +
                        "DbVH0wATE1bGrItvVar6nWLttA/4uUE63a7OP9TZGCB4YjGTzpWHtlpl7AC+qeTmt0tScH/Zf3Fm8fG1uhVZBE" +
                        "5xdZkXmgkXzJaNJ94jeijM= -----END CERTIFICATE-----");
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

    @Test
    void testSaslSslPlainMaasCredExtractor() {
        MaasTopicDefinition topicDefinition = MaasTopicDefinition.builder()
                .setName("/org/qubership/maas/declarative/kafka/client/impl/common/cred/extractor/SASL_SSL_PLAIN.json")// Path in resource folder
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
        assertThat(extractedCreds.get("sasl.mechanism")).isEqualTo("PLAIN");
        assertThat(extractedCreds.get("sasl.jaas.config"))
                .isEqualTo("org.apache.kafka.common.security.plain.PlainLoginModule required username=\"alice\" password=\"alice-secret\";");
        assertThat(extractedCreds.get("ssl.keystore.key"))
                .isEqualTo("-----BEGIN RSA PRIVATE KEY----- MIIEvgIBADANBgkqhkiG9w0BAQEFAASCBKgwggSkAgEAAoIBAQD" +
                        "OcPkCmbOb37+FdYGN0AZFkzc64bcNCSa1njvjYnDMiWlA+sQU8giIKit2w5QR1IHQTsKVOzDY4dKiDOzHSnwiM" +
                        "uqZtHuNBqOgT6F2wyDWfUk0uSC8KTgHxiJspwgcSvtGXt8Z+Akk4NsfGySwCg69qG1IFJAwIiL8+NLjt50H2Y+" +
                        "sUyzGlwL2Z5FvxOKfQDftkcTiv4/cJlQp/Q/73emt00GkJ16DUi1J83Qa2Mwq8pPAjOER3UwsJXR6OTDxnyZbu" +
                        "ZEIbwHIeLW++u+X0An8/ViYNyJm5VYmIFnUbFRm6gq0I35cXIBmOp5zyaZPfBUGgDlklsVLGdeJYXCPt6l5AgM" +
                        "BAAECggEAdUY8TzvdipkrBbfhJxyb0350C6EaCe++68G+J8hOaANPpbhPF87/Bcte8ZLdVNux+xIi+/+qGir/K" +
                        "0vIXtZzekIOf87FuTGiVnmmMf9bcj2uDqN0cy+/QrtN5wdkVh5KbImMmAYNu8N490zGJLMx+I/rtNzppE5gLfF" +
                        "0bCRssn3h1ctSooqqjGI1+fu59Ee9kFMvWdlndqPZqthUu39ZhL6a3xfpIUuqfMPEvG9UZiSRNWcrPwZD56sR1" +
                        "DNRDJhRg4WpTpyjcSFGV9SzD69v9/P5mKix46da04ORQGgOyWl/VIUNON69BNUeSNhcWMSPWKpS4SNE+0x6Ib+" +
                        "y99Ot4QKBgQDacHPxTWWIlcV7IIDSDAmF7URfUwD81SoIBwiO309CkwOZxTpUWZNOr6wauFwgZa+ltyT7V3jHP" +
                        "MzqWKo1rzjGO1XZLRT/7xToiZwNXsvF+FyMoADQoJommFOzI2XTHl+Xt58H3DfVQ5vb01QaU7nE/Y5/Gdl0o5B" +
                        "dwJBgzeZQVQKBgQDx8GCcidai4npFiKuUQPaDoLflq9la2T/7/y9sHOaF9f7Jpg35nFakWEB9e7VWs7+AycxzO" +
                        "YXSMjP9QczGpLvKZFv70a8ewhopUGoSbAxXOHYLNlfimSLcThF9edQGkyU2h+whQBJBPr4mMa326fiUIM6NZOx" +
                        "HTIcWM74JgMdIlQKBgQCT13BhbQS/QL776q/Feign4MJZ0g/lD1D3Retg53r5kA7r0DT+SfINW5CcNRMFSk1rV" +
                        "11MT5OLJXd7VK8+zm0anaYo6v4/ik0YZaqc0gAQ6VuaQeDLUyeB368mm1qbXP9N7BVSeJXBXZTrX0iY2Pnufb4" +
                        "oABLRgrJ/AYm2OyUaUQKBgANu59AQ3KzfDeEiuVyXN2hSxYlK0QDLCG9pBaFBH7xS61StOJGMOGaIohGlycL7N" +
                        "gJ72pzf+hgrgjVb064V9FxA5FVEenmzQ2/GplqwkdRDBtnN4Z1jku0RZoojSnD1cZe+gglOwXOPkl80YUWZuF9" +
                        "Rv9ydzASO/3O/NDkqTKmpAoGBAL+QJfrzQ/Q9EiiXmOI3LUK/UeAKQAyf3J4QFRNFKLixlbl3Xa/Fa2y2YlVAb" +
                        "gnYxUcRcsaUmv77MNL5CxwUvC9hEnQAsGcPRjKNx6Qjuy/piuHjgueGLrY+nrgeTN/j+J111V6Db0kg/vU1PlE" +
                        "SKHfT4qvZ7FWRLl5B22qVRR4h -----END RSA PRIVATE KEY-----");
        assertThat(extractedCreds.get("ssl.keystore.certificate.chain"))
                .isEqualTo("-----BEGIN CERTIFICATE----- MIID/TCCAuWgAwIBAgIQHO0KyuduE+XLmgQniMWsRjANBgkqhkiG9w0" +
                        "BAQsFADCBiTEVMBMGA1UEBhMMQW5raC1Nb3Jwb3JrMRowGAYDVQQKExFVbnNlZW4gVW5pdmVyc2l0eTEQMA4GA" +
                        "1UECxMHTGlicmFyeTFCMEAGA1UEAxM5TGFuZG9vcCdzIEZhc3QgRGF0YSBEZXYgU2VsZiBTaWduZWQgQ2VydGl" +
                        "maWNhdGUgQXV0aG9yaXR5MB4XDTIwMTAwMjEwMjQxNVoXDTMwMDkzMDEwMjQxNVowVjEVMBMGA1UEBhMMQW5ra" +
                        "C1Nb3Jwb3JrMRowGAYDVQQKExFVbnNlZW4gVW5pdmVyc2l0eTEQMA4GA1UECxMHTGlicmFyeTEPMA0GA1UEAxM" +
                        "GY2xpZW50MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEAznD5Apmzm9+/hXWBjdAGRZM3OuG3DQkmt" +
                        "Z4742JwzIlpQPrEFPIIiCordsOUEdSB0E7ClTsw2OHSogzsx0p8IjLqmbR7jQajoE+hdsMg1n1JNLkgvCk4B8Y" +
                        "ibKcIHEr7Rl7fGfgJJODbHxsksAoOvahtSBSQMCIi/PjS47edB9mPrFMsxpcC9meRb8Tin0A37ZHE4r+P3CZUK" +
                        "f0P+93prdNBpCdeg1ItSfN0GtjMKvKTwIzhEd1MLCV0ejkw8Z8mW7mRCG8ByHi1vvrvl9AJ/P1YmDciZuVWJiB" +
                        "Z1GxUZuoKtCN+XFyAZjqec8mmT3wVBoA5ZJbFSxnXiWFwj7epeQIDAQABo4GSMIGPMA4GA1UdDwEB/wQEAwIFo" +
                        "DAdBgNVHSUEFjAUBggrBgEFBQcDAQYIKwYBBQUHAwIwDAYDVR0TAQH/BAIwADBQBgNVHREESTBHggw3ZDJlNDQ" +
                        "1NzViYmSCBWthZmthghhsb2NhbGthZmthLmthZmthLWNsdXN0ZXKHBH8AAAGHBH8AAAGHBMCoY2SHBKwftoAwD" +
                        "QYJKoZIhvcNAQELBQADggEBAG4N5VRLUbYd39KR/bfVMftfhSKd3MnXgIT6goWL36ShmqD7MKnoRIyVmlSe2Ko" +
                        "0laJiVvFrhzd/W6U8O7J5OMEdKjF3VZPrFTOz/oSOFr6PO7jt3h6Z91Tq2lVkupoilacw0DCirgA1EYIxZsqKs" +
                        "jje941qZxHuJ4AssNox3P2eU5tWo/caSHPG2P3KBmCnC9eVVU9B5iD894DKr1ueFYg3fmDIDcXYu/Dq32oAO80" +
                        "DbVH0wATE1bGrItvVar6nWLttA/4uUE63a7OP9TZGCB4YjGTzpWHtlpl7AC+qeTmt0tScH/Zf3Fm8fG1uhVZBE" +
                        "5xdZkXmgkXzJaNJ94jeijM= -----END CERTIFICATE-----");
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
