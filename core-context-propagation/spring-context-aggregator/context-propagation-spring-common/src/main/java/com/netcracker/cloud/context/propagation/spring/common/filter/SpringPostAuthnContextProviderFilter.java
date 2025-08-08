package com.netcracker.cloud.context.propagation.spring.common.filter;

import com.netcracker.cloud.context.propagation.core.ContextInitializationStep;
import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import org.springframework.core.annotation.Order;
import org.springframework.web.filter.OncePerRequestFilter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;

@Order(0)
public class SpringPostAuthnContextProviderFilter extends OncePerRequestFilter {

    @Override
    protected void doFilterInternal(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, FilterChain filterChain) throws ServletException, IOException {
        RequestContextPropagation.initRequestContext(new SpringRequestContextData(httpServletRequest), ContextInitializationStep.POST_AUTHENTICATION);
        RequestContextPropagation.setResponsePropagatableData(new SpringResponseContextData(httpServletResponse));
        filterChain.doFilter(httpServletRequest, httpServletResponse);
    }
}
