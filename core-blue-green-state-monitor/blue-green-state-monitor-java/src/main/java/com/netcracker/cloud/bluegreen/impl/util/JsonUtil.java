package com.netcracker.cloud.bluegreen.impl.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.SneakyThrows;

public class JsonUtil {
    private static final ObjectMapper objectMapper = new ObjectMapper()
            .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    @SneakyThrows
    public static String toJson(Object object) {
        return objectMapper.writeValueAsString(object);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, Class<T> type) {
        return objectMapper.readValue(json, type);
    }

    @SneakyThrows
    public static <T> T fromJson(String json, TypeReference<T> type) {
        return objectMapper.readValue(json, type);
    }

    @SneakyThrows
    public static <T> T fromJson(byte[] json, Class<T> type) {
        return objectMapper.readValue(json, type);
    }

    @SneakyThrows
    public static <T> T fromJson(byte[] json, TypeReference<T> type) {
        return objectMapper.readValue(json, type);
    }

}
