package com.netcracker.cloud.context.propagation.quarkus.runtime.filter;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.core.UriInfo;
import org.jboss.resteasy.reactive.common.util.QuarkusMultivaluedHashMap;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.util.Collections;
import java.util.List;
import java.util.Map;


public class QuarkusRequestContextDataTest {


    @Test
    public void getAll() {
        ContainerRequestContext containerRequestContext = Mockito.mock(ContainerRequestContext.class);
        String acceptLanguage = "Accept-Language";
        String acceptLanguageValue = "ru;en";

        String xVersion = "X-Version";
        String xVersionValue = "v1";

        MultivaluedMap<String, String> multivaluedMap = new QuarkusMultivaluedHashMap<>();
        multivaluedMap.add(acceptLanguage, acceptLanguageValue);
        multivaluedMap.add(xVersion, xVersionValue);

        Mockito.when(containerRequestContext.getHeaders())
                .thenReturn(multivaluedMap);
        UriInfo uriInfo = Mockito.mock(UriInfo.class);
        Mockito.when(uriInfo.getPath()).thenReturn("/test/path");
        Mockito.when(containerRequestContext.getUriInfo())
                .thenReturn(uriInfo);


        QuarkusRequestContextData quarkusRequestContextData = new QuarkusRequestContextData(containerRequestContext);
        Map<String, List<?>> headers = quarkusRequestContextData.getAll();
        Assertions.assertEquals(3, headers.size());
        Assertions.assertEquals(Collections.singletonList(acceptLanguageValue), headers.get(acceptLanguage));
        Assertions.assertEquals(Collections.singletonList(xVersionValue), headers.get(xVersion));

    }
}
