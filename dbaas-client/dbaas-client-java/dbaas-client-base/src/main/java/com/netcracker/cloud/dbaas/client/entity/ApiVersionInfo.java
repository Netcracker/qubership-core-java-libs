package com.netcracker.cloud.dbaas.client.entity;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Collections;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ApiVersionInfo {
    private Integer major;
    private Integer minor;
    private List<Integer> supportedMajors;
    private List<Info> specs;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Info {
        private String specRootUrl;
        private Integer major;
        private Integer minor;
        private List<Integer> supportedMajors;
    }

    public List<Integer> getSupportedMajors() {
        if (specs != null) {
            // Find the spec entry with specRootUrl = "/api"
            for (Info spec : specs) {
                if ("/api".equals(spec.getSpecRootUrl())) {
                    return spec.getSupportedMajors();
                }
            }
        } else if (supportedMajors != null) {
            return supportedMajors;
        }
        return Collections.emptyList();
    }
}
