package org.qubership.cloud.restlegacy.resttemplate;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.qubership.cloud.security.common.restclient.OAuthRestTemplateProvider;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.web.client.RestTemplate;

import java.util.Collections;
import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(SpringRunner.class)
public class RestTemplateCreatorTest {

    List<ClientHttpRequestInterceptor> clientHttpRequestInterceptorList;
    @Mock
    private OAuthRestTemplateProvider oAuthRestTemplateProvider;
    private RestTemplateCreator restTemplateCreator;

    @Mock
    RestTemplateBuilder restTemplateBuilder;

    @Before
    public void setUp() {
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
    public void createRestTemplate() {
        RestTemplate expectedRestTemplate = getExpectedRestTemplate();
        when(restTemplateBuilder.build()).thenReturn(new RestTemplate());
        RestTemplate restTemplate = restTemplateCreator.create();
        Assert.assertEquals(expectedRestTemplate.getInterceptors(), restTemplate.getInterceptors());
    }

    @Test
    public void createRestTemplateWithM2M() {
        RestTemplate expectedRestTemplate = getExpectedRestTemplate();
        ClientRegistration clientRegistration = ClientRegistration.withRegistrationId("test-id")
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .clientId("test")
                .tokenUri("http://localhost")
                .build();
//        Mockito.when(oAuthRestTemplateProvider.getOAuthRestTemplate())
//                .thenReturn(new MicroserviceClientOAuthRestTemplate(Mockito.mock(OAuth2AuthorizedClientManager.class),
//                        clientRegistration));

        RestTemplate restTemplate = restTemplateCreator.withM2M().create();
        Assert.assertEquals(expectedRestTemplate.getInterceptors(), restTemplate.getInterceptors());
    }
}
