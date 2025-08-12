package com.netcracker.cloud.disableapi.spring;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.ArrayList;
import java.util.List;

@ConfigurationProperties(prefix = "deprecated.api")
@Getter
@Setter
public class DeprecatedApiProperties {
    private List<String> patterns;

    public DeprecatedApiProperties(List<String> patterns) {
        if(patterns == null) patterns = new ArrayList<>();
        this.patterns = patterns;
    }

}
