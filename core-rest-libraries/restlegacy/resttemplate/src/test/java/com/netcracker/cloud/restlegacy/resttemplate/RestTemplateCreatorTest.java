package com.netcracker.cloud.restlegacy.resttemplate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import com.netcracker.cloud.security.common.restclient.OAuthRestTemplateProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RestTemplateCreatorTest {

    List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList;
    @Mock
    private OAuthRestTemplateProvider oAuthRestTemplateProvider;
    private RestTemplateCreator restTemplateCreator;

    @Mock
    private RestTemplateBuilder restTemplateBuilder;

    @BeforeEach
    void setUp() {
        clientHttpRequestInterceptorList = Collections.singletonList(mock(ClientHttpRequestInterceptor.class));

        RestTemplateFactory restTemplateFactory = new RestTemplateFactory(oAuthRestTemplateProvider,
                mock(ClientHttpRequestFactory.class),
                clientHttpRequestInterceptorList, restTemplateBuilder);
        restTemplateCreator = restTemplateFactory.getRestTemplateCreator();
    }

    private RestTemplate getExpectedRestTemplate() {
        RestTemplate expectedRestTemplate = new RestTemplate();
        expectedRestTemplate.setInterceptors(clientHttpRequestInterceptorList);
        return expectedRestTemplate;
    }


    @Test
    void createRestTemplate() {
        RestTemplate expectedRestTemplate = getExpectedRestTemplate();
        when(restTemplateBuilder.build()).thenReturn(new RestTemplate());
        RestTemplate restTemplate = restTemplateCreator.create();
        assertEquals(expectedRestTemplate.getInterceptors(), restTemplate.getInterceptors());
    }

    @Test
    void createRestTemplateWithM2M() {
        RestTemplate expectedRestTemplate = getExpectedRestTemplate();
        Mockito.when(oAuthRestTemplateProvider.getOAuthRestTemplate()).thenReturn(expectedRestTemplate);

        RestTemplate restTemplate = restTemplateCreator.withM2M().create();
        assertEquals(expectedRestTemplate.getInterceptors(), restTemplate.getInterceptors());
    }
}
