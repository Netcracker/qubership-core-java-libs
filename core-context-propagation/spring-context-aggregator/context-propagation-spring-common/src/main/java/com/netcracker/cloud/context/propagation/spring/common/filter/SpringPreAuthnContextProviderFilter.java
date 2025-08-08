package org.qubership.cloud.context.propagation.spring.common.filter;

import org.qubership.cloud.context.propagation.core.ContextInitializationStep;
import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Order(-1)
public class SpringPreAuthnContextProviderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        RequestContextPropagation.clear();
        try {
            RequestContextPropagation.initRequestContext(new SpringRequestContextData(httpServletRequest), ContextInitializationStep.PRE_AUTHENTICATION);
            filterChain.doFilter(httpServletRequest, httpServletResponse);
        } finally {
            RequestContextPropagation.clear();
        }
    }
}
