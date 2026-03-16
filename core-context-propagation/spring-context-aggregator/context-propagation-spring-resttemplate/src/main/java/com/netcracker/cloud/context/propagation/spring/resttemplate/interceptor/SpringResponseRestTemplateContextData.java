package com.netcracker.cloud.context.propagation.spring.resttemplate.interceptor;

import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpRequest;

import java.util.List;
import java.util.Map;
import java.util.TreeMap;

public class SpringResponseRestTemplateContextData implements OutgoingContextData {

    private static final Logger log = LoggerFactory.getLogger(SpringResponseRestTemplateContextData.class);

    private Map<String, Object> responseHeaders = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

    public Map<String, Object> getResponseHeaders(){
        return responseHeaders;
    }

    public HttpRequest addHeadersToRequest(HttpRequest httpRequest){
        for(Map.Entry<String, Object> entry : responseHeaders.entrySet()){
            if(!httpRequest.getHeaders().containsHeader(entry.getKey())) {
                log.trace("Add header={} with value={} from context", entry.getKey(), entry.getValue());

                if (entry.getValue() instanceof String)
                    httpRequest.getHeaders().add(entry.getKey(), (String) entry.getValue());

                if (entry.getValue() instanceof List && ((List) entry.getValue()).get(0) instanceof String)
                    httpRequest.getHeaders().addAll(entry.getKey(), ((List<String>) entry.getValue()));
            }
        }
        return httpRequest;
    }

    @Override
    public void set(String name, Object values) {
        responseHeaders.put(name, values);
    }
}
