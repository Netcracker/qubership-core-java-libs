package org.qubership.cloud.springcloud.config.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.util.List;

@Data
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown=true)
public class CloudEnv {
    private String name;
    private List<String> profiles;
    private List<PropertySource> propertySources;
}
