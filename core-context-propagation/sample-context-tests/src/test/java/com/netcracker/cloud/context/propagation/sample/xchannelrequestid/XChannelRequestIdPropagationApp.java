package com.netcracker.cloud.context.propagation.sample.xchannelrequestid;

import com.netcracker.cloud.context.propagation.spring.resttemplate.annotation.EnableResttemplateContextProvider;
import com.netcracker.cloud.context.propagation.spring.resttemplate.interceptor.SpringRestTemplateInterceptor;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@SpringBootApplication(
        scanBasePackages = "com.netcracker.cloud.context.propagation.sample.xchannelrequestid"
)
@EnableResttemplateContextProvider
public class XChannelRequestIdPropagationApp {

    public static void main(String[] args) {
        SpringApplication.run(XChannelRequestIdPropagationApp.class, args);
    }

    @Bean
    public RestTemplate restTemplate(SpringRestTemplateInterceptor interceptor) {
        RestTemplate rt = new RestTemplate();
        rt.setInterceptors(List.of(interceptor));
        return rt;
    }
}
