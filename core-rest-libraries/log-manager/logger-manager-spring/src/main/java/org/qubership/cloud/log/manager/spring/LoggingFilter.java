package org.qubership.cloud.log.manager.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.log.manager.common.LogManager;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.util.Map;

public class LoggingFilter implements Filter {

    public static final String ENDPOINT = "/api/logging/v1/levels";
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();

    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestUri = urlPathHelper.getRequestUri(httpServletRequest);
        String httpMethod = httpServletRequest.getMethod().toUpperCase();
        HttpServletResponse httpServletResponse = (HttpServletResponse) response;
        if (requestUri.equals(ENDPOINT) && httpMethod.equals("GET")) {
            Map<String, String> logLevels = LogManager.getLogLevel();
            httpServletResponse.setContentType("application/json");
            httpServletResponse.setStatus(HttpServletResponse.SC_OK);
            objectMapper.writeValue(response.getWriter(), logLevels);
            return;
        }
        chain.doFilter(request, response);
    }
}