package org.qubership.cloud.context.propagation.spring.common.filter;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import jakarta.servlet.http.HttpServletRequest;
import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SpringRequestContextDataTest {

    @Test
    void getAll() {
        HttpServletRequest httpServletRequest = Mockito.mock(HttpServletRequest.class);
        String acceptLanguage = "Accept-Language";
        String xVersion = "X-Version";
        Mockito.when(httpServletRequest.getHeaderNames())
                .thenReturn(new EnumerationImpl<>(Arrays.asList(acceptLanguage, xVersion).iterator()));
        String acceptLanguageValue = "ru;en";

        Mockito.when(httpServletRequest.getHeaders(acceptLanguage))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(acceptLanguageValue).iterator()));
        String xVersionValue = "v1";
        Mockito.when(httpServletRequest.getHeaders(xVersion))
                .thenReturn(new EnumerationImpl<>(Collections.singletonList(xVersionValue).iterator()));


        SpringRequestContextData springRequestContextData = new SpringRequestContextData(httpServletRequest);
        Map<String, List<?>> headers = springRequestContextData.getAll();
        assertEquals(2, headers.size());
        assertEquals(acceptLanguageValue, headers.get(acceptLanguage).get(0));
        assertEquals(xVersionValue, headers.get(xVersion).get(0));

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