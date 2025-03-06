package org.qubership.cloud.framework.contexts.utils;

import org.apache.commons.lang3.StringUtils;

public class PropertyUtils {
    private PropertyUtils() {}

    public static String getPropertyOrEnv(final String propertyName) {
        return getPropertyOrEnv(propertyName, formatPropertyToEnv(propertyName));
    }

    public static String getPropertyOrEnv(final String propertyName, final String envName) {
        String propertyValue = System.getProperty(propertyName);
        return StringUtils.isBlank(propertyValue) ? System.getenv(envName) : propertyValue;
    }

    public static String getPropertyOrEnvOrDefault(final String propertyName, final String envName, String defaultValue) {
        String actualValue = getPropertyOrEnv(propertyName, envName);
        return StringUtils.isBlank(actualValue) ? defaultValue : actualValue;
    }

    private static String formatPropertyToEnv(final String propertyName) {
        return propertyName.toUpperCase().replaceAll("\\.", "_");
    }
}
