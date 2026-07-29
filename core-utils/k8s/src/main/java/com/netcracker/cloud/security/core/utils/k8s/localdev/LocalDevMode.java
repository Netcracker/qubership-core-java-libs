package com.netcracker.cloud.security.core.utils.k8s.localdev;

import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@NoArgsConstructor(access = AccessLevel.PRIVATE)
public final class LocalDevMode {

    public static final String ENABLED_PROPERTY = "security.local-dev.enabled";
    public static final String ENABLED_ENV = "SECURITY_LOCAL_DEV_ENABLED";

    public static final String MICROSERVICE_NAME_PROPERTY = "cloud.microservice.name";
    public static final String MICROSERVICE_NAME_ENV = "CLOUD_MICROSERVICE_NAME";

    public static final String NAMESPACE_ENV = "CLOUD_NAMESPACE";

    public static final String QUARKUS_PROFILE_PROPERTY = "quarkus.profile";
    public static final String QUARKUS_PROFILE_ENV = "QUARKUS_PROFILE";

    public static final String SPRING_PROFILES_ACTIVE_PROPERTY = "spring.profiles.active";
    public static final String SPRING_PROFILES_ACTIVE_ENV = "SPRING_PROFILES_ACTIVE";

    public static final String DEV_PROFILE = "dev";

    public static boolean isEnabled() {
        if (isTrue(firstNonBlank(System.getProperty(ENABLED_PROPERTY), System.getenv(ENABLED_ENV)))) {
            return true;
        }
        if (DEV_PROFILE.equalsIgnoreCase(firstNonBlank(
                System.getProperty(QUARKUS_PROFILE_PROPERTY),
                System.getenv(QUARKUS_PROFILE_ENV)))) {
            return true;
        }
        return containsProfile(firstNonBlank(
                System.getProperty(SPRING_PROFILES_ACTIVE_PROPERTY),
                System.getenv(SPRING_PROFILES_ACTIVE_ENV)), DEV_PROFILE);
    }

    public static String requireMicroserviceName() {
        String name = firstNonBlank(
                System.getProperty(MICROSERVICE_NAME_PROPERTY),
                System.getenv(MICROSERVICE_NAME_ENV));
        if (StringUtils.isBlank(name)) {
            throw new IllegalStateException(
                    "Local-dev M2M requires '" + MICROSERVICE_NAME_PROPERTY
                            + "' (system property or " + MICROSERVICE_NAME_ENV
                            + " env). Set it in application config and ensure framework bootstrap runs, "
                            + "or pass -D" + MICROSERVICE_NAME_PROPERTY + "=<service-account-name>.");
        }
        return name.trim();
    }

    public static String requireNamespace() {
        String namespace = System.getenv(NAMESPACE_ENV);
        if (StringUtils.isBlank(namespace)) {
            throw new IllegalStateException(
                    "Local-dev M2M requires env '" + NAMESPACE_ENV
                            + "' with the Kubernetes namespace of the service account.");
        }
        return namespace.trim();
    }

    private static boolean containsProfile(String profiles, String expected) {
        if (StringUtils.isBlank(profiles)) {
            return false;
        }
        for (String profile : profiles.split(",")) {
            if (expected.equalsIgnoreCase(profile.trim())) {
                return true;
            }
        }
        return false;
    }

    private static boolean isTrue(String value) {
        return "true".equalsIgnoreCase(value) || "1".equals(value);
    }

    private static String firstNonBlank(String first, String second) {
        return LocalDevJsonUtils.firstNonBlank(first, second);
    }
}
