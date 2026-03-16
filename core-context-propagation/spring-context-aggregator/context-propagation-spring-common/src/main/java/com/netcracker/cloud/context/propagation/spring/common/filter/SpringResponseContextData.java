package com.netcracker.cloud.context.propagation.spring.common.filter;

import com.netcracker.cloud.context.propagation.core.contextdata.OutgoingContextData;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.http.HttpServletResponse;

public class SpringResponseContextData implements OutgoingContextData {

    private static final Logger log = LoggerFactory.getLogger(SpringResponseContextData.class);

    private HttpServletResponse httpServletResponse;

    public SpringResponseContextData(HttpServletResponse httpServletResponse) {
        this.httpServletResponse = httpServletResponse;
    }

    @Override
    public void set(String name, Object values) {
        if (!httpServletResponse.containsHeader(name)) {
            httpServletResponse.setHeader(name, values.toString());
        }
    }
}
