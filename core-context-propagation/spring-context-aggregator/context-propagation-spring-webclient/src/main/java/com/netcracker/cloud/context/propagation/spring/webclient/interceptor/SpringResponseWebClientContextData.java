package com.netcracker.cloud.context.propagation.spring.webclient.interceptor;

import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.reactive.function.client.ClientRequest;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SpringResponseWebClientContextData implements OutgoingContextData {

    private static final Logger log = LoggerFactory.getLogger(SpringResponseWebClientContextData.class);


    private Map<String, Object> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public Map<String, Object> getResponseHeaders(){
        return responseHeaders;
    }

    public ClientRequest addHeadersToRequest(ClientRequest clientRequest){
        return ClientRequest.from(clientRequest)
                .headers(headers -> responseHeaders.forEach((headerKey, headerValue) -> {
                    if (!headers.containsHeader(headerKey)) {
                        log.trace("Add header={} with value={} from context", headerKey, headerValue);
                        if (headerValue instanceof String)
                            headers.add(headerKey, (String) headerValue);
                        if (headerValue instanceof List && ((List) headerValue).get(0) instanceof String)
                            headers.addAll(headerKey, ((List<String>) headerValue));
                    }
                }))
                .build();
    }

    @Override
    public void set(String name, Object values) {
        responseHeaders.put(name, values);
    }
}
