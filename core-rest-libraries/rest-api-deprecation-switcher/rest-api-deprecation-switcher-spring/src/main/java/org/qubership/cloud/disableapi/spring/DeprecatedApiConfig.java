package org.qubership.cloud.disableapi.spring;

import org.qubership.cloud.disableapi.UrlsPatternsParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
@Slf4j
@EnableConfigurationProperties(DeprecatedApiProperties.class)
@ConditionalOnProperty(name = "deprecated.api.disabled", havingValue = "true")
public class DeprecatedApiConfig {
    private final DeprecatedApiProperties deprecatedApiProperties;

    public DeprecatedApiConfig(DeprecatedApiProperties deprecatedApiProperties) {
        this.deprecatedApiProperties = deprecatedApiProperties;
    }

    @Bean
    public FilterRegistrationBean<DeprecatedApiFilter> deprecatedApiFilter(@Autowired RequestMappingHandlerMapping requestMappingHandlerMapping) {
        FilterRegistrationBean<DeprecatedApiFilter> apiFilterRegBean = new FilterRegistrationBean<>();
        Map<String, Set<String>> pathsMapFromProperties = UrlsPatternsParser.parse(deprecatedApiProperties.getPatterns());
        Map<String, Set<String>> pathMapsFromAnnotations = new DeprecatedApi().getDeprecatedApi(requestMappingHandlerMapping.getHandlerMethods());
        Map<String, Set<String>> pathsMap;
        if (!pathsMapFromProperties.isEmpty() && !pathMapsFromAnnotations.isEmpty()) {
            throw new IllegalStateException("Found deprecated REST endpoints both from 'deprecated.api.patterns' property and " +
                    "from the code annotated with @Deprecated annotation! Cannot use both approaches simultaneously.");
        } else if (!pathsMapFromProperties.isEmpty()) {
            log.info("Using paths from 'deprecated.api.patterns' property");
            pathsMap = pathsMapFromProperties;
        } else {
            log.info("Using paths annotated by @Deprecated annotation");
            pathsMap = pathMapsFromAnnotations;
        }
        log.warn("Disabling the following deprecated paths: \n{}",
                pathsMap.entrySet().stream()
                        .map(entry -> String.format("%s %s", entry.getKey(),
                                entry.getValue().stream().sorted().collect(Collectors.toList())))
                        .sorted()
                        .collect(Collectors.joining("\n")));
        apiFilterRegBean.setFilter(new DeprecatedApiFilter(pathsMap, new ErrorHandler()));
        apiFilterRegBean.setOrder(HIGHEST_PRECEDENCE);
        return apiFilterRegBean;
    }
}
