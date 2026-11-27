package com.netcracker.cloud.security.core.utils.tls;

import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.net.ssl.SSLContext;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;
import java.security.KeyStore;
import java.util.Comparator;
import java.util.ServiceConfigurationError;
import java.util.ServiceLoader;

@SuppressWarnings("UnusedReturnValue")
public class TlsUtils {
    private static final Logger log = LoggerFactory.getLogger(TlsUtils.class);
    private static final TlsConfig INSTANCE = getTlsConfig();

    private TlsUtils() {
    }

    @NotNull
    private static TlsConfig getTlsConfig() {
        TlsConfig tlsConfig = selectTlsConfig(TlsUtils.class.getClassLoader(),
                Thread.currentThread().getContextClassLoader());
        if (tlsConfig == null) {
            throw new Error("Service loader failed to load TlsConfig service");
        }
        log.info("Using TlsConfig implementation {}", tlsConfig.getClass().getName());
        tlsConfig.load();
        return tlsConfig;
    }

    // Chooses the TlsConfig with the highest priority.
    // Providers visible only through the thread context classloader are added as well,but they are ignored if they
    // cannot be loaded as a TlsConfig of this library (e.g. when their TlsConfig type comes from another classloader).
    static TlsConfig selectTlsConfig(ClassLoader owner, ClassLoader contextLoader) {
        TlsConfig best = findHighestPriority(owner);
        if (contextLoader != null && contextLoader != owner) {
            TlsConfig fromContext = tryFindHighestPriority(contextLoader);
            if (fromContext != null && (best == null || fromContext.priority() > best.priority())) {
                best = fromContext;
            }
        }
        return best;
    }

    private static TlsConfig findHighestPriority(ClassLoader classLoader) {
        return ServiceLoader.load(TlsConfig.class, classLoader)
                .stream()
                .map(ServiceLoader.Provider::get)
                .max(Comparator.comparingInt(TlsConfig::priority))
                .orElse(null);
    }

    private static TlsConfig tryFindHighestPriority(ClassLoader classLoader) {
        try {
            return findHighestPriority(classLoader);
        } catch (ServiceConfigurationError e) {
            log.warn("Ignoring TlsConfig providers visible through the thread context classloader: {}",
                    e.getMessage());
            return null;
        }
    }

    public static SSLContext createSSLContext(KeyStore trustStore, KeyStore keyStore, String keyPassword) {
        return INSTANCE.createSSLContext(trustStore, keyStore, keyPassword);
    }

    public static boolean isInternalTlsEnabled() {
        return INSTANCE.isInternalTlsEnabled();
    }

    public static String getKeyStoreType() {
        return INSTANCE.getKeyStoreType();
    }

    public static String getTrustStoreType() {
        return INSTANCE.getTrustStoreType();
    }

    public static String getKeyStorePath() {
        return INSTANCE.getKeyStorePath();
    }

    public static String getTrustStorePath() {
        return INSTANCE.getTrustStorePath();
    }

    public static String getCaCertificatePath() {
        return INSTANCE.getCaCertificatePath();
    }

    public static String getCertificateStorePassword() {
        return INSTANCE.getCertificateStorePassword();
    }

    public static KeyStore getKeyStore() {
        return INSTANCE.getKeyStore();
    }

    public static KeyStore getTrustStore() {
        return INSTANCE.getTrustStore();
    }

    public static X509KeyManager getKeyManager() {
        return INSTANCE.getKeyManager();
    }

    public static X509TrustManager getTrustManager() {
        return INSTANCE.getTrustManager();
    }

    public static SSLContext getSslContext() {
        return INSTANCE.getSslContext();
    }

    public static String selectUrl(String httpUrl, String httpsUrl) {
        return INSTANCE.isInternalTlsEnabled() ? httpsUrl : httpUrl;
    }
}
