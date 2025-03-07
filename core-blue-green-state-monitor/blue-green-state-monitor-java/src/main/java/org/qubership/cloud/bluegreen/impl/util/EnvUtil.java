package org.qubership.cloud.bluegreen.impl.util;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;
import java.util.Optional;

public class EnvUtil {

    public static final String NAMESPACE_PROP = "cloud.microservice.namespace";
    public static final String MICROSERVICE_PROP = "cloud.microservice.name";
    public static final String CONSUL_URL_PROP = "consul.url";
    public static final String POD_NAME_PROP = "pod.name";

    public static final String NAMESPACE_ENV = "NAMESPACE";
    public static final String CLOUD_NAMESPACE_ENV = "CLOUD_NAMESPACE";
    public static final String MICROSERVICE_NAME_ENV = "SERVICE_NAME";
    public static final String CONSUL_URL_ENV = "CONSUL_URL";
    public static final String POD_NAME_ENV = "POD_NAME";

    public static String getConsulUrl() {
        return getPropOrEnvsMust(CONSUL_URL_PROP, CONSUL_URL_ENV);
    }

    public static String getNamespace() {
        return getPropOrEnvsMust(NAMESPACE_PROP, NAMESPACE_ENV, CLOUD_NAMESPACE_ENV);
    }

    public static String getMicroserviceName() {
        return getPropOrEnvsMust(MICROSERVICE_PROP, MICROSERVICE_NAME_ENV);
    }

    public static String getPodName() {
        return getPropOrEnvs(POD_NAME_PROP, POD_NAME_ENV).orElseGet(() -> {
                    try {
                        return InetAddress.getLocalHost().getHostName();
                    } catch (UnknownHostException e) {
                        throw new IllegalArgumentException("Failed to resolve hostname", e);
                    }
                });
    }

    private static String getPropOrEnvsMust(String prop, String... envs) {
        return getPropOrEnvs(prop, envs)
                .orElseThrow(() -> {
                    String msg = String.format("Missing required prop: '%s' or env(s): '%s'", prop, String.join(",", envs));
                    return new IllegalStateException(msg);
                });
    }

    private static Optional<String> getPropOrEnvs(String prop, String... envs) {
        return Optional.ofNullable(System.getProperty(prop)).or(() -> Arrays.stream(envs).map(System::getenv).filter(Objects::nonNull).findFirst());
    }


}
