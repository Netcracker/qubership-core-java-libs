package org.qubership.cloud.springcloud.config.source;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.quarkus.runtime.annotations.RegisterForReflection;
import lombok.Data;

import java.util.Map;


@Data
@RegisterForReflection
@JsonIgnoreProperties(ignoreUnknown=true)
public class PropertySource {
    private String name;
    private Map<String,String> source;
}
