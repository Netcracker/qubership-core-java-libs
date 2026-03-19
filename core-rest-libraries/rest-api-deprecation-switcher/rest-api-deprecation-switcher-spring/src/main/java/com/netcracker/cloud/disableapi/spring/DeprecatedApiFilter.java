package com.netcracker.cloud.disableapi.spring;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.core.error.rest.tmf.TmfErrorResponse;
import com.netcracker.cloud.disableapi.UrlsPatternsParser;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Getter;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.util.UrlPathHelper;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.Map;
import java.util.Set;

@Getter
public class DeprecatedApiFilter implements Filter {
    private final Map<String, Set<String>> urlPatternsToHttpMethods;
    private final ErrorHandler errorHandler;
    private final UrlPathHelper urlPathHelper = new UrlPathHelper();
    private final AntPathMatcher antPathMatcher = new AntPathMatcher();
    private final ObjectMapper objectMapper = new ObjectMapper();

    public DeprecatedApiFilter(Map<String, Set<String>> urlPatternsToHttpMethods, ErrorHandler errorHandler) {
        this.urlPatternsToHttpMethods = urlPatternsToHttpMethods;
        this.errorHandler = errorHandler;
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        HttpServletRequest httpServletRequest = (HttpServletRequest) request;
        String requestUri = urlPathHelper.getRequestUri(httpServletRequest);
        String httpMethod = httpServletRequest.getMethod().toUpperCase();
        Map.Entry<String, Set<String>> deprecatedPathMethods = urlPatternsToHttpMethods.entrySet()
                .stream()
                .filter(entry -> antPathMatcher.match(entry.getKey(), requestUri) &&
                        (entry.getValue().contains(UrlsPatternsParser.WILDCARD) || entry.getValue().contains(httpMethod)))
                .findFirst().orElse(null);
        if (deprecatedPathMethods != null) {
            String pathPattern = deprecatedPathMethods.getKey();
            Set<String> httpMethods = deprecatedPathMethods.getValue();
            ResponseEntity<TmfErrorResponse> disabledApiResponse = errorHandler.buildErrorResponse(httpServletRequest, httpMethods, pathPattern);
            HttpServletResponse httpServletResponse = (HttpServletResponse) response;
            httpServletResponse.setStatus(disabledApiResponse.getStatusCode().value());
            httpServletResponse.setContentType(MediaType.APPLICATION_JSON_VALUE);
            PrintWriter out = httpServletResponse.getWriter();
            objectMapper.writeValue(out, disabledApiResponse.getBody());
            out.flush();
            return;
        }
        chain.doFilter(request, response);
    }
}
