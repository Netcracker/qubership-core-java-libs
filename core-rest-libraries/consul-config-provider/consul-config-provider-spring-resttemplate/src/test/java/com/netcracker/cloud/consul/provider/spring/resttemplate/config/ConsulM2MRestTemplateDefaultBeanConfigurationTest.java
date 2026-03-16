package com.netcracker.cloud.consul.provider.spring.resttemplate.config;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

class ConsulM2MRestTemplateDefaultBeanConfigurationTest {

    @Test
    void defaultbeanConsulTokenStorageViaM2MRestTemplate() {
        ConsulM2MRestTemplateDefaultBeanConfiguration consulM2MRestTemplateDefaultBeanConfiguration = new ConsulM2MRestTemplateDefaultBeanConfiguration();
        Assertions.assertEquals("", consulM2MRestTemplateDefaultBeanConfiguration.defaultbeanConsulTokenStorageViaM2MRestTemplate().get());
        consulM2MRestTemplateDefaultBeanConfiguration.defaultbeanConsulTokenStorageViaM2MRestTemplate().update("");
    }
}
