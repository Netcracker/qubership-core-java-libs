package com.netcracker.cloud.context.propagation.spring.common.filter;

import org.qubership.cloud.context.propagation.core.contextdata.IncomingContextData;

import jakarta.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class SpringRequestContextData implements IncomingContextData {

    private final HttpServletRequest httpServletRequest;

    public SpringRequestContextData(HttpServletRequest httpServletRequest) {
        this.httpServletRequest = httpServletRequest;
        httpServletRequest.setAttribute("cloud-core.context-propagation.url", httpServletRequest.getRequestURI());
    }

    @Override
    public Object get(String name) {
        Object data = httpServletRequest.getHeader(name);
        return data == null ? httpServletRequest.getAttribute(name) : data;
    }

    @Override
    public Map<String, List<?>> getAll() {
        return Collections.list(httpServletRequest.getHeaderNames())
                .stream()
                .collect(Collectors.toMap(headerName -> headerName,
                        headerName -> Collections.list(httpServletRequest.getHeaders((String) headerName))));
    }
}
