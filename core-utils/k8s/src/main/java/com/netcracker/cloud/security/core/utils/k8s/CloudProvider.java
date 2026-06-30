package com.netcracker.cloud.security.core.utils.k8s;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.HttpURLConnection;
import java.net.URISyntaxException;
import java.net.URI;

import java.io.IOException;

/**
 * Identifies which managed Kubernetes cloud provider (if any) the JVM is
 * currently running on, detected by probing the cloud instance metadata service.
 */
public enum CloudProvider {
    GKE,
    EKS,
    AKS,
    ON_PREM;

    private static final Logger log = LoggerFactory.getLogger(CloudProvider.class);

    /**
     * Link-local address reserved by IANA for cloud instance metadata services.
     * Used by AWS (IMDSv1/v2), GCP, and Azure — not a general-purpose IP.
     * Sonar S1313 suppressed because this is an infrastructure constant, not a hardcoded endpoint.
     */
    @SuppressWarnings("java:S1313")
    private static final String DEFAULT_METADATA_URL = "http://169.254.169.254";

    private static final int CONNECT_TIMEOUT_MS = 10_000;
    private static final int READ_TIMEOUT_MS = 10_000;

    // Lazily computed and cached on first access.
    static volatile CloudProvider detected;

    /**
     * Returns the detected cloud provider, computing and caching it on first call.
     */
    public static CloudProvider getCloudProvider() {
        CloudProvider result = detected;
        if (result == null) {
            synchronized (CloudProvider.class) {
                result = detected;
                if (result == null) {
                    result = detect(DEFAULT_METADATA_URL);
                    log.info("Detected cloud provider: {}", result);
                    detected = result;
                }
            }
        }
        return result;
    }

    static CloudProvider detect(String metadataUrl) {
        if (isGke(metadataUrl)) {
            return GKE;
        }
        if (isEks(metadataUrl)) {
            return EKS;
        }
        if (isAks(metadataUrl)) {
            return AKS;
        }
        return ON_PREM;
    }

    private static boolean isGke(String metadataUrl) {
        try {
            HttpURLConnection conn = openConnection(metadataUrl + "/computeMetadata/v1/");
            conn.setRequestProperty("Metadata-Flavor", "Google");
            int code = conn.getResponseCode();
            String flavor = conn.getHeaderField("Metadata-Flavor");
            log.debug("GKE probe: status={} Metadata-Flavor={}", code, flavor);
            return code == 200 && "Google".equals(flavor);
        } catch (Exception e) {
            log.debug("GKE probe failed: {}", e.toString());
            return false;
        }
    }

    private static boolean isEks(String metadataUrl) {
        try {
            HttpURLConnection conn = openConnection(metadataUrl + "/latest/meta-data/");
            int code = conn.getResponseCode();
            log.debug("EKS probe: status={}", code);
            // 200 = IMDSv1 open, 401 = IMDSv2 required — both confirm EC2/EKS
            return code == 200 || code == 401;
        } catch (Exception e) {
            log.debug("EKS probe failed: {}", e.toString());
            return false;
        }
    }

    private static boolean isAks(String metadataUrl) {
        try {
            HttpURLConnection conn = openConnection(metadataUrl + "/metadata/instance?api-version=2021-02-01");
            conn.setRequestProperty("Metadata", "true");
            int code = conn.getResponseCode();
            log.debug("AKS probe: status={}", code);
            return code == 200;
        } catch (Exception e) {
            log.debug("AKS probe failed: {}", e.toString());
            return false;
        }
    }

    private static HttpURLConnection openConnection(String url) throws URISyntaxException, IOException {
        HttpURLConnection conn = (HttpURLConnection) new URI(url).toURL().openConnection();
        conn.setConnectTimeout(CONNECT_TIMEOUT_MS);
        conn.setReadTimeout(READ_TIMEOUT_MS);
        conn.setRequestMethod("GET");
        conn.setInstanceFollowRedirects(false);
        return conn;
    }
}
