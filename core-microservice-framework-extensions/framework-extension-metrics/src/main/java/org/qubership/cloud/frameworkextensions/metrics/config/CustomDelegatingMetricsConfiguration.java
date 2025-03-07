package org.qubership.cloud.frameworkextensions.metrics.config;

import com.ryantenney.metrics.spring.config.annotation.DelegatingMetricsConfiguration;
import com.ryantenney.metrics.spring.config.annotation.MetricsConfigurer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.type.AnnotationMetadata;

import java.util.List;

// TODO com.ryantenney.metrics.spring.config.annotation.DelegatingMetricsConfiguration has error in Spring 4.3.x, WA - CustomDelegatingMetricsConfiguration
@Configuration
public class CustomDelegatingMetricsConfiguration extends DelegatingMetricsConfiguration {

    @Override
    @Autowired(required = false)
    public void setMetricsConfigurers(final List<MetricsConfigurer> configurers) {
    }

    @Override
    public void setImportMetadata(AnnotationMetadata importMetadata) {
    }
}