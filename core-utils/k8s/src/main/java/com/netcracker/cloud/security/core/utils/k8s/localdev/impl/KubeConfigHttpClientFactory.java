package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.http.HttpClient;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.time.Duration;
import java.util.Collection;

class KubeConfigHttpClientFactory {

    private static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(30);

    HttpClient create(KubeConfigCredentials credentials) {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .sslContext(createSslContext(credentials))
                .build();
    }

    HttpClient createInsecureForLocalDev() {
        return HttpClient.newBuilder()
                .version(HttpClient.Version.HTTP_1_1)
                .connectTimeout(CONNECT_TIMEOUT)
                .sslContext(createInsecureSslContext())
                .build();
    }

    /**
     * Local-dev only: the kube API / IdP uses a non-standard (private or self-signed) certificate,
     * so TLS verification is skipped.
     */
    private SSLContext createInsecureSslContext() {
        try {
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, new TrustManager[]{new InsecureTrustManager()}, new SecureRandom());
            return sslContext;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("Failed to create insecure SSL context for local-dev IdP", e);
        }
    }

    private SSLContext createSslContext(KubeConfigCredentials credentials) {
        try {
            if (credentials.isInsecureSkipTlsVerify()) {
                return createInsecureSslContext();
            }
            if (credentials.getCertificateAuthorityData() == null
                    || credentials.getCertificateAuthorityData().length == 0) {
                return SSLContext.getDefault();
            }

            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            Collection<? extends Certificate> certificates =
                    certificateFactory.generateCertificates(
                            new ByteArrayInputStream(credentials.getCertificateAuthorityData()));

            KeyStore keyStore = KeyStore.getInstance(KeyStore.getDefaultType());
            keyStore.load(null, null);
            int index = 0;
            for (Certificate certificate : certificates) {
                keyStore.setCertificateEntry("ca-" + index++, certificate);
            }

            TrustManagerFactory trustManagerFactory =
                    TrustManagerFactory.getInstance(TrustManagerFactory.getDefaultAlgorithm());
            trustManagerFactory.init(keyStore);

            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, trustManagerFactory.getTrustManagers(), new SecureRandom());
            return sslContext;
        } catch (GeneralSecurityException | IOException e) {
            throw new IllegalStateException("Failed to create SSL context from kubeconfig CA", e);
        }
    }

    /**
     * Local-dev only: accepts any server certificate when kubeconfig sets insecure-skip-tls-verify
     * or the IdP uses a private CA not in the JVM trust store.
     */
    private static final class InsecureTrustManager implements X509TrustManager {
        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType) { // NOSONAR
            // Local-dev: client certificates are not used for TokenRequest / OIDC discovery.
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType) { // NOSONAR
            // Local-dev: kubeconfig insecure-skip-tls-verify or private IdP CA — validation intentionally skipped.
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return new X509Certificate[0];
        }
    }
}
