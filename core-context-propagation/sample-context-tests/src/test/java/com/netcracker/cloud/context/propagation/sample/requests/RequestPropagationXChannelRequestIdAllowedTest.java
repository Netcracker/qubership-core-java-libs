package com.netcracker.cloud.context.propagation.sample.requests;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import com.netcracker.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;
import com.netcracker.cloud.framework.contexts.allowedheaders.HeaderPropagationConfiguration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.context.WebApplicationContext;

import static jakarta.ws.rs.core.HttpHeaders.ACCEPT_LANGUAGE;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.header;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

@SpringBootTest
@ContextConfiguration(classes = {
        TestController.class, RequestPropagationTestConfig.class})
@TestPropertySource(properties = {
        "headers.allowed=custom-header",
        "headers.blocked=",
        "cloud-core.context-propagation.url=/test_url/v111/test"
})
class RequestPropagationXChannelRequestIdAllowedTest {
    @Autowired
    protected WebApplicationContext context;

    @Autowired
    SpringPreAuthnContextProviderFilter preAuthnFilter;
    @Autowired
    SpringPostAuthnContextProviderFilter postAuthnFilter;

    private MockMvc mockMvc;

    public static String X_REQUEST_ID_NAME = "x-request-id";
    public static String X_CHANNEL_REQUEST_ID_NAME = "x-channel-request-id";
    public static String X_VERSION_NAME = "x-version";
    public static String CUSTOM_NAME = "custom-header";

    public static String ACCEPT_LANGUAGE_VALUE = "ru";
    public static String X_REQUEST_ID_VALUE = "123";
    public static String X_CHANNEL_REQUEST_ID_VALUE = "456";
    public static String X_VERSION_VALUE = "v123";
    public static String CUSTOM_VALUE = "value";

    @Autowired
    RestTemplate restTemplate;

    @BeforeAll
    static void beforeAll() {
        System.setProperty("headers.allowed", "custom-header");
        System.setProperty("headers.blocked", "");
        HeaderPropagationConfiguration.resetCache();
        ContextManager.reinitialize();
    }

    @AfterAll
    static void afterAll() {
        System.clearProperty("headers.allowed");
        System.clearProperty("headers.blocked");
        HeaderPropagationConfiguration.resetCache();
        ContextManager.reinitialize();
    }

    @BeforeEach
    void setUp() {
        System.setProperty("headers.allowed", "custom-header");
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(preAuthnFilter, postAuthnFilter).build();
    }

    private void sendRequest(String path) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(path)
                .header(X_REQUEST_ID_NAME, X_REQUEST_ID_VALUE)
                .header(X_CHANNEL_REQUEST_ID_NAME, X_CHANNEL_REQUEST_ID_VALUE)
                .header(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE)
                .header(X_VERSION_NAME, X_VERSION_VALUE)
                .header(CUSTOM_NAME, CUSTOM_VALUE);
        mockMvc.perform(requestBuilder);
    }

    @Test
    void testRequestPropagationAllowsXChannelRequestIdWhenBlockedHeadersEmpty() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo("/chain_request"))
                .andExpect(header(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE))
                .andExpect(header(X_REQUEST_ID_NAME, X_REQUEST_ID_VALUE))
                .andExpect(header(X_CHANNEL_REQUEST_ID_NAME, X_CHANNEL_REQUEST_ID_VALUE))
                .andExpect(header(CUSTOM_NAME, CUSTOM_VALUE))
                .andRespond(withSuccess());

        sendRequest("/test");
    }
}
