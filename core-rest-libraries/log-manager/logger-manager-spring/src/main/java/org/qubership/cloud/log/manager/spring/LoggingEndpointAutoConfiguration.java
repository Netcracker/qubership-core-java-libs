package org.qubership.cloud.log.manager.spring;

import org.springframework.boot.web.servlet.FilterRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import static org.springframework.core.Ordered.HIGHEST_PRECEDENCE;

@Configuration
public class LoggingEndpointAutoConfiguration {

    @Bean
    public FilterRegistrationBean<LoggingFilter> loggingFilterRegistration() {
        FilterRegistrationBean<LoggingFilter> registrationBean = new FilterRegistrationBean<>();
        registrationBean.setFilter(new LoggingFilter());
        registrationBean.addUrlPatterns(LoggingFilter.ENDPOINT);
        registrationBean.setOrder(HIGHEST_PRECEDENCE);
        return registrationBean;
    }

    @Bean
    public LoggingUpdater loggingUpdater(Environment environment) {
        return new LoggingUpdater(environment);
    }

}