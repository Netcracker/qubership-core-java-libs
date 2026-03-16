package com.netcracker.cloud.context.propagation.spring.common.filter;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.headerstracking.filters.context.AcceptLanguageContext;
import com.netcracker.cloud.headerstracking.filters.context.RequestIdContext;
import jakarta.servlet.Servlet;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.mock.web.MockFilterChain;

import java.io.IOException;
import java.util.*;

import static org.junit.jupiter.api.Assertions.*;
import static com.netcracker.cloud.context.propagation.core.ContextManager.getSafe;
import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static org.mockito.Mockito.*;

class SpringContextProviderFilterTest {
    public static final String X_REQUEST_ID_CONTEXT_NAME = "X-Request-Id";

    @BeforeEach
    void init() {
        ContextManager.clearAll();
    }

    @Test
    void testDoFilterInternal() throws ServletException, IOException {
        HttpServletRequest httpServletRequest = mock(HttpServletRequest.class);
        when(httpServletRequest.getHeaderNames())
                .thenReturn(new EnumerationImpl<>(Arrays.asList(ACCEPT_LANGUAGE, X_REQUEST_ID_CONTEXT_NAME).iterator()));

        String acceptLanguageValue = "ru;en";
        when(httpServletRequest.getHeaders(ACCEPT_LANGUAGE))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(acceptLanguageValue).iterator()));

        String xRequestIdValue = "123";
        when(httpServletRequest.getHeaders(X_REQUEST_ID_CONTEXT_NAME))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(xRequestIdValue).iterator()));
        when(httpServletRequest.getHeader(X_REQUEST_ID_CONTEXT_NAME))
                .thenReturn(xRequestIdValue);


        HttpServletResponse httpServletResponse = mock(HttpServletResponse.class);
        Servlet servlet = mock(Servlet.class);

        assertNull(AcceptLanguageContext.get());
        assertNotEquals(xRequestIdValue, RequestIdContext.get());

        FilterChain filterChain = new MockFilterChain(servlet, new SpringPreAuthnContextProviderFilter(), new SpringPostAuthnContextProviderFilter());
        filterChain.doFilter(httpServletRequest, httpServletResponse);

        try {
            getSafe(RequestIdContext.get());
        } catch (Exception e) {
            assertEquals("Context with name " + RequestIdContext.get() + " is not registered and context provider can not be found", e.getMessage());
        }
        assertNotEquals(xRequestIdValue, RequestIdContext.get());
        assertNull(AcceptLanguageContext.get());
    }

    public static class EnumerationImpl<T> implements Enumeration<T> {
        private final Iterator<T> iterator;

        public EnumerationImpl(Iterator<T> iterator) {
            this.iterator = iterator;
        }

        public boolean hasMoreElements() {
            return this.iterator.hasNext();
        }

        public T nextElement() {
            return this.iterator.next();
        }
    }

}
