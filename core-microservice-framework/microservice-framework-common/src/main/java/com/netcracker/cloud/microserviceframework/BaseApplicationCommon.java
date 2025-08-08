package com.netcracker.cloud.microserviceframework;

import org.qubership.cloud.frameworkextensions.metrics.config.annotation.EnableCustomMetrics;
import org.qubership.cloud.microserviceframework.config.MicroserviceFrameworkConfiguration;
import org.qubership.cloud.microserviceframework.config.annotation.EnableMSInfoProvider;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.cloud.config.client.ConfigClientAutoConfiguration;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

import static org.springframework.context.annotation.FilterType.REGEX;

@Configuration
@EnableAutoConfiguration(exclude = ConfigClientAutoConfiguration.class)
@ComponentScan(value = "org.qubership.cloud",
        excludeFilters = {
                @ComponentScan.Filter(type = REGEX, pattern = "org.qubership.cloud.microserviceframework.*"),
                @ComponentScan.Filter(type = REGEX, pattern = "org.qubership.cloud.securitycore.*"),
                @ComponentScan.Filter(type = REGEX, pattern = "org.qubership.cloud.dbaas.client.*")})
@EnableCustomMetrics
@EnableMSInfoProvider
@SpringBootConfiguration(proxyBeanMethods = false)
@Import({MicroserviceFrameworkConfiguration.class})
abstract class BaseApplicationCommon {
}
