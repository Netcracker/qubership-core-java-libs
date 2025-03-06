package org.qubership.cloud.context.propagation.spring.resttemplate.interceptor;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestExecution;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.http.client.ClientHttpResponse;

import java.io.IOException;


public class SpringRestTemplateInterceptor implements ClientHttpRequestInterceptor {

    @Override
    public ClientHttpResponse intercept(HttpRequest httpRequest, byte[] bytes, ClientHttpRequestExecution clientHttpRequestExecution) throws IOException {
        SpringResponseRestTemplateContextData springResponseContextData = new SpringResponseRestTemplateContextData();
        RequestContextPropagation.populateResponse(springResponseContextData);
        return clientHttpRequestExecution.execute(springResponseContextData.addHeadersToRequest(httpRequest), bytes);
    }
}
