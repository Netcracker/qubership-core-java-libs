package org.qubership.maas.declarative.kafka.client;

import org.junit.platform.commons.util.StringUtils;
import org.yaml.snakeyaml.Yaml;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public class TestUtils {
    public static String readResourceAsString(String filename) {
        StackTraceElement[] stackTrace = Thread.currentThread().getStackTrace();
        String path = Arrays.stream(stackTrace)
                .skip(1)
                .dropWhile(e -> e.getClassName().startsWith(TestUtils.class.getName()))
                .findFirst()
                .orElseThrow()
                .getClassName()
                .replaceAll("\\.", "/") + "/" + filename;
        URL url = Thread.currentThread().getContextClassLoader().getResource(path);
        if (url == null) {
            throw new RuntimeException("No resource found by classpath:" + path);
        }


        try {
            return Files.readString(Paths.get(url.toURI()), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new RuntimeException(e);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    public static Map<String, Object> loadConfig(String filename) {
        Map<String, Object> tree = new Yaml().load(TestUtils.readResourceAsString(filename));
        Map<String, Object> config = new HashMap<>();
        flatten(config, "", tree);

        return config;
    }

    private static void flatten(Map<String, Object> result, String path, Map<String, Object> tree) {
        for (var e : tree.entrySet()) {
            if (e.getValue() instanceof Map) {
                flatten(result, join(path, e.getKey()), (Map)e.getValue());
            } else {
                result.put(join(path, e.getKey()), e.getValue());
            }
        }
    }

    private static String join(String path, String key) {
        return StringUtils.isBlank(path) ? key : path + "." + key;
    }
}
