package com.netcracker.cloud.restclient.resttemplate;


import org.springframework.http.HttpHeaders;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class HeaderUtils {
    public static Map<String, List<String>> toMap(HttpHeaders headers) {
        if (headers == null) {
            return Map.of();
        }
        return headers.headerSet().stream()
                .collect(Collectors.toMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue
                ));
    }
}
