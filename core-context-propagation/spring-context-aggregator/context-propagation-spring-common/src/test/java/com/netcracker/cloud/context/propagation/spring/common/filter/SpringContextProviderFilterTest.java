package com.netcracker.cloud.context.propagation.spring.common.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.headerstracking.filters.context.AcceptLanguageContext;
import com.netcracker.cloud.headerstracking.filters.context.ChannelRequestIdContext;
import com.netcracker.cloud.headerstracking.filters.context.RequestIdContext;
import com.netcracker.cloud.framework.contexts.acceptlanguage.AcceptLanguageProvider;
import com.netcracker.cloud.framework.contexts.xchannelrequestid.XChannelRequestIdContextProvider;
import com.netcracker.cloud.framework.contexts.xrequestid.XRequestIdContextProvider;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.FilterChain;
import org.springframework.mock.web.MockFilterChain;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static org.mockito.Mockito.*;

class SpringContextProviderFilterTest {
    public static final String X_REQUEST_ID_CONTEXT_NAME = "X-Request-Id";
    public static final String X_CHANNEL_REQUEST_ID_CONTEXT_NAME = "X-Channel-Request-Id";

    @BeforeEach
    void init() {
        ContextManager.clearAll();
    }

    @Test
    void testDoFilterInternal() throws ServletException, IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeaderNames())
                .thenReturn(new EnumerationImpl<>(Arrays.asList(ACCEPT_LANGUAGE, X_REQUEST_ID_CONTEXT_NAME, X_CHANNEL_REQUEST_ID_CONTEXT_NAME).iterator()));

        String acceptLanguageValue = "ru;en";
        when(httpServletRequest.getHeaders(ACCEPT_LANGUAGE))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(acceptLanguageValue).iterator()));

        String xRequestIdValue = "123";
        when(httpServletRequest.getHeaders(X_REQUEST_ID_CONTEXT_NAME))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(xRequestIdValue).iterator()));
        when(httpServletRequest.getHeader(X_REQUEST_ID_CONTEXT_NAME))
                .thenReturn(xRequestIdValue);

        String xChannelRequestIdValue = "456";
        when(httpServletRequest.getHeaders(X_CHANNEL_REQUEST_ID_CONTEXT_NAME))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(xChannelRequestIdValue).iterator()));
        when(httpServletRequest.getHeader(X_CHANNEL_REQUEST_ID_CONTEXT_NAME))
                .thenReturn(xChannelRequestIdValue);


        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);

        ContextManager.register(Arrays.asList(
                new AcceptLanguageProvider(),
                new XRequestIdContextProvider(),
                new XChannelRequestIdContextProvider()
        ));

        assertNull(AcceptLanguageContext.get());
        assertNotEquals(xRequestIdValue, RequestIdContext.get());
        assertNotEquals(xChannelRequestIdValue, ChannelRequestIdContext.get());

        Servlet servlet = new HttpServlet() {
            @Override
            public void service(ServletRequest req, ServletResponse res) {
                assertEquals(xRequestIdValue, RequestIdContext.get());
                assertEquals(xChannelRequestIdValue, ChannelRequestIdContext.get());
                assertEquals(acceptLanguageValue, AcceptLanguageContext.get());
            }
        };

        FilterChain filterChain = new MockFilterChain(servlet, new SpringPreAuthnContextProviderFilter(), new SpringPostAuthnContextProviderFilter());
        filterChain.doFilter(httpServletRequest, httpServletResponse);

        assertNull(AcceptLanguageContext.get());
    }
}
