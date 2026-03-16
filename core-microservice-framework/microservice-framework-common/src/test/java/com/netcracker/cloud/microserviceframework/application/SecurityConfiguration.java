package com.netcracker.cloud.microserviceframework.application;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.autoconfigure.security.SecurityProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class SecurityConfiguration {
    public static final String TEST_TENANT_ID = "test";
    public static final String CUSTOM_ROLE = "some_custom_role";
    public static final String ALLOWED_HEADER = "allowed_header";
    private static final String UUID = "/[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}";

//    @Bean
//    public HttpSecurityConfigurer authorizationConfigurer() {
//        return httpSecurity -> httpSecurity.authorizeRequests()
//                .anyRequest().permitAll();
//    }

    @Bean
    @ConditionalOnMissingBean
    public SecurityProperties securityProperties() {
        return new SecurityProperties();
    }


}
