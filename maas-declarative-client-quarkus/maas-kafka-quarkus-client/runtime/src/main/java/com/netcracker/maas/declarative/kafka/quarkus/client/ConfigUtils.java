package org.qubership.maas.declarative.kafka.quarkus.client;

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
                try {
                    Optional<Integer> intVal = config.getOptionalValue(key, Integer.class);
                    if (intVal.isPresent() && intVal.get() instanceof Integer) {
                        map.put(mapKey, intVal.get());
                        continue;
                    }
                } catch (IllegalArgumentException | ClassCastException ex) {
                }

                try {
                    Optional<Double> doubleVal = config.getOptionalValue(key, Double.class);
                    if (doubleVal.isPresent() && doubleVal.get() instanceof Double) {
                        map.put(mapKey, doubleVal.get());
                        continue;
                    }
                } catch (IllegalArgumentException | ClassCastException ex) {
                }

                try {
                    Optional<String> strVal = config.getOptionalValue(key, String.class);
                    if (strVal.isPresent()) {
                        String val = strVal.get().trim();
                        if (val.equalsIgnoreCase("false")) {
                            map.put(mapKey, false);
                        } else if (val.equalsIgnoreCase("true")) {
                            map.put(mapKey, true);
                        } else {
                            map.put(mapKey, val);
                        }
                        continue;
                    }
                } catch (IllegalArgumentException | ClassCastException ex) {
                }

                try {
                    Optional<Boolean> boolVal = config.getOptionalValue(key, Boolean.class);
                    boolVal.ifPresent(v -> map.put(mapKey, boolVal));
                } catch (IllegalArgumentException | ClassCastException ex) {
                }
            }
        }

        return map;
    }

}
