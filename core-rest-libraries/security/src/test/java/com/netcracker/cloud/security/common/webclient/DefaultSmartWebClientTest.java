package com.netcracker.cloud.security.common.webclient;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.function.Consumer;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class DefaultSmartWebClientTest {

    private WebClient.Builder builder;
    private WebClient.Builder clonedBuilder;
    private WebClient webClient;

    private ExchangeFilterFunction genericInterceptor1;
    private ExchangeFilterFunction genericInterceptor2;
    private ExchangeFilterFunction m2mAuth;
    private ExchangeFilterFunction userAuth;
    private ExchangeFilterFunction securityContextInterceptor;
    private AuthorizationHeaderInterceptorFactory factory;

    private DefaultSmartWebClient client;

    @BeforeEach
    void setUp() {
        builder = mock(WebClient.Builder.class);
        clonedBuilder = mock(WebClient.Builder.class);
        webClient = mock(WebClient.class);

        when(builder.clone()).thenReturn(clonedBuilder);
        when(clonedBuilder.build()).thenReturn(webClient);
        when(clonedBuilder.filter(any())).thenReturn(clonedBuilder);
        when(clonedBuilder.filters(any())).thenReturn(clonedBuilder);

        genericInterceptor1 = mock(ExchangeFilterFunction.class);
        genericInterceptor2 = mock(ExchangeFilterFunction.class);

        m2mAuth = mock(ExchangeFilterFunction.class);
        userAuth = mock(ExchangeFilterFunction.class);
        securityContextInterceptor = mock(ExchangeFilterFunction.class);

        factory = mock(AuthorizationHeaderInterceptorFactory.class);
        when(factory.build(Mode.M2M)).thenReturn(m2mAuth);
        when(factory.build(Mode.USER)).thenReturn(userAuth);

        client = new DefaultSmartWebClient(
                List.of(genericInterceptor1, genericInterceptor2),
                securityContextInterceptor,
                factory,
                builder
        );
    }

    @Test
    void getWebClientBuilderForMode_DEFAULT_withSecurityContext_shouldIncludeInterceptor() {
        client.getWebClientBuilderForMode(Mode.DEFAULT);

        verify(builder).clone();
        verify(clonedBuilder).filters(any());
        verify(clonedBuilder).filter(securityContextInterceptor);
    }

    @Test
    void getWebClientBuilderForMode_DEFAULT_withoutSecurityContext_shouldNotIncludeInterceptor() {
        client = new DefaultSmartWebClient(
                List.of(genericInterceptor1),
                null,
                factory,
                builder
        );

        client.getWebClientBuilderForMode(Mode.DEFAULT);

        verify(builder).clone();
        verify(clonedBuilder).filters(any());
        verify(clonedBuilder, never()).filter(any());
    }

    @Test
    void getWebClientBuilderForMode_M2M_shouldApplyM2mAuthHeader() {
        client.getWebClientBuilderForMode(Mode.M2M);

        verify(clonedBuilder).filter(m2mAuth);
        verify(factory).build(Mode.M2M);
    }

    @Test
    void getWebClientBuilderForMode_USER_shouldApplyUserAuthHeader() {
        client.getWebClientBuilderForMode(Mode.USER);

        verify(clonedBuilder).filter(userAuth);
        verify(factory).build(Mode.USER);
    }

    @Test
    void getWebClientBuilder_shouldDelegateToDefaultMode() {
        client.getWebClientBuilder();

        verify(builder).clone();
        verify(clonedBuilder).filters(any());
    }

    @Test
    void getWebClientForM2mAuthorization_shouldBuildWebClient() {
        client.getWebClientForM2mAuthorization();

        verify(clonedBuilder).filter(m2mAuth);
        verify(clonedBuilder).build();
    }

    @Test
    void getWebClientForUserAuthorization_shouldBuildWebClient() {
        client.getWebClientForUserAuthorization();

        verify(clonedBuilder).filter(userAuth);
        verify(clonedBuilder).build();
    }

    @Test
    void prepareWebClient_shouldApplyAllGenericInterceptors() {
        ArgumentCaptor<Consumer<List<ExchangeFilterFunction>>> captor = ArgumentCaptor.forClass(java.util.function.Consumer.class);

        client.getWebClientBuilderForMode(Mode.M2M);

        verify(clonedBuilder).filters(captor.capture());

        List<ExchangeFilterFunction> filtersList = mock(List.class);
        captor.getValue().accept(filtersList);

        verify(filtersList, times(2)).add(any());
    }
}
