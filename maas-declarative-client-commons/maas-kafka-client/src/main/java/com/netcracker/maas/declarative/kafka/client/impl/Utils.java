package com.netcracker.maas.declarative.kafka.client.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class Utils {
    private static final Logger LOG = LoggerFactory.getLogger(Utils.class);

    public static Map<String, Object> merge(Map<String, Object> mapA, Map<String, Object> mapB) {
        Map<String, Object> mergedMap = new HashMap<>();
        if (mapA != null) {
            mergedMap.putAll(mapA);
        }
        if (mapB != null) {
            mergedMap.putAll(mapB);
        }
        return mergedMap;
    }

    public static Map<String, Object> prepareConfigAsMap(Map<String, Object> config, String prefix) {
        Map<String, Object> map = new HashMap<>();
        Iterable<String> propNames = config.keySet();

        for (String propKey : propNames) {
            String key = propKey.toLowerCase().replace("_", ".");
            if (key.startsWith(prefix)) {
                String mapKey = key.substring(prefix.length());
                try {
                    Object val = config.get(key);
                    if (val instanceof Integer) {
                        map.put(mapKey, val);
                        continue;
                    }
                } catch (IllegalArgumentException ex) {
                    // Ignore it
                }

                try {
                    Object val = config.get(key);
                    if (val instanceof Double) {
                        map.put(mapKey, val);
                        continue;
                    }
                } catch (IllegalArgumentException ex) {
                    // Ignore it
                }

                try {
                    Object objVal = config.get(key);
                    if (objVal instanceof String) {
                        String val = ((String)objVal).trim();
                        if (val.equalsIgnoreCase("false")) {
                            map.put(mapKey, false);
                        } else if (val.equalsIgnoreCase("true")) {
                            map.put(mapKey, true);
                        } else {
                            map.put(mapKey, val);
                        }
                        continue;
                    }
                } catch (IllegalArgumentException ex) {
                    // Ignore it
                }

                try {
                    Object val = config.get(key);
                    if (val instanceof Boolean) {
                        map.put(mapKey, val);
                        continue;
                    }
                } catch (IllegalArgumentException ex) {
                    // Ignore it
                }

                // Log property that can't be correctly processed
                LOG.warn("Property {} can't be casted correctly", key);
            }
        }

        return map;
    }

    @FunctionalInterface
    public interface OmnivoreRunnable {
        void run() throws Exception;
    }

    public static void safe(OmnivoreRunnable runnable) {
        try {
            runnable.run();
        } catch (Exception e) {
            LOG.debug("suppress execution error", e);
        }
    }

}
