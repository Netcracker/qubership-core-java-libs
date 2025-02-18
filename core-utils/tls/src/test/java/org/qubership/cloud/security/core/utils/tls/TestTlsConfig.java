package org.qubership.cloud.security.core.utils.tls;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;

public class TestTlsConfig implements TlsConfig {

    @Override
    public void load() {

    }

    @Override
    public SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword) {
        return null;
    }

    @Override
    public boolean isInternalTlsEnabled() {
        return false;
    }

    @Override
    public String getKeyStoreType() {
        return null;
    }

    @Override
    public String getTrustStoreType() {
        return null;
    }

    @Override
    public String getKeyStorePath() {
        return null;
    }

    @Override
    public String getTrustStorePath() {
        return null;
    }

    @Override
    public String getCaCertificatePath() {
        return null;
    }

    @Override
    public String getCertificateStorePassword() {
        return null;
    }

    @Override
    public KeyStore getKeyStore() {
        return null;
    }

    @Override
    public KeyStore getTrustStore() {
        return null;
    }

    @Override
    public X509KeyManager getKeyManager() {
        return null;
    }

    @Override
    public X509TrustManager getTrustManager() {
        return null;
    }

    @Override
    public SSLContext getSslContext() {
        return null;
    }

    @Override
    public int priority() {
        return -1;
    }
}
