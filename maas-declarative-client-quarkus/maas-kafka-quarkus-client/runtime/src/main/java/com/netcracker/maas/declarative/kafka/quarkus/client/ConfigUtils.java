package com.netcracker.maas.declarative.kafka.quarkus.client;

import org.eclipse.microprofile.config.Config;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class ConfigUtils {

    public static Map<String, Object> configAsMap(Config config, String prefix) {
        Map<String, Object> map = new HashMap<>();
        Iterable<String> propNames = config.getPropertyNames();

        for (String propKey : propNames) {
            String key = propKey.replace("_", ".");
            if (key.startsWith(prefix)) {
                String mapKey = key.substring(prefix.length());
                putIfPresent(config, key, mapKey, map, Integer.class, Long.class, Double.class, Boolean.class);
                getOptionalPropertyOfType(config, key, String.class)
                        .map(String::trim)
                        .ifPresent(val -> map.put(mapKey, switch(val.toLowerCase()) {
                            case "true" -> true;
                            case "false" -> false;
                            default -> val;
                        }));
            }
        }

        return map;
    }

    private static <T> Optional<T> getOptionalPropertyOfType(Config config, String key, Class<T> propertyType) {
        try {
            return config.getOptionalValue(key, propertyType);
        } catch (IllegalArgumentException | ClassCastException ex) {
            return Optional.empty();
        }
    }

    private static void putIfPresent(Config config, String key, String mapKey, Map<String, Object> map, Class<?>... types) {
        for (Class<?> type : types) {
            getOptionalPropertyOfType(config, key, type).ifPresent(value -> map.put(mapKey, value));
        }
    }
}
