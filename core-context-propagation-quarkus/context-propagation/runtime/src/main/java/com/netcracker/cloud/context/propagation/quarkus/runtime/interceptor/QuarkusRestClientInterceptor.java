package com.netcracker.cloud.context.propagation.quarkus.runtime.interceptor;

import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
public class QuarkusRestClientInterceptor implements ClientRequestFilter {

    @Override
    public void filter(ClientRequestContext requestContext) {
        QuarkusResponseContextData quarkusResponseContextData = new QuarkusResponseContextData();
        RequestContextPropagation.populateResponse(quarkusResponseContextData);
        quarkusResponseContextData.addHeadersToMap(requestContext.getHeaders());
    }
}
