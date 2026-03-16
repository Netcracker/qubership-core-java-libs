package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.headerstracking.filters.context.AcceptLanguageContext;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

public class QuarkusPostAuthnContextProviderFilterTest {
    @BeforeAll
    static void init() {
        ContextManager.clearAll();
    }

    @Test
    public void testDoFilter() {
        assertNull(AcceptLanguageContext.get());
        ContainerRequestContext containerRequestContext = Mockito.mock(ContainerRequestContext.class);
        String acceptLanguage = "Accept-Language";
        String acceptLanguageValue = "ru;en";

        MultivaluedMap<String, String> multivaluedMap = new QuarkusMultivaluedHashMap<>();
        multivaluedMap.add(acceptLanguage, acceptLanguageValue);

        Mockito.when(containerRequestContext.getHeaders())
                .thenReturn(multivaluedMap);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getPath()).thenReturn("/test/path");
        Mockito.when(containerRequestContext.getUriInfo())
                .thenReturn(uriInfo);

        QuarkusPostAuthnContextProviderFilter quarkusFilter = new QuarkusPostAuthnContextProviderFilter();
        quarkusFilter.filter(containerRequestContext);
        assertEquals(acceptLanguageValue, AcceptLanguageContext.get());
    }

}
