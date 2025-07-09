package org.qubership.cloud.context.propagation.sample.requests;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPostAuthnContextProviderFilter;
import org.qubership.cloud.context.propagation.spring.common.filter.SpringPreAuthnContextProviderFilter;
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
        "cloud-core.context-propagation.url=/test_url/v111/test"
})
class RequestPropagationTest {
    @Autowired
    protected WebApplicationContext context;

    @Autowired
    SpringPreAuthnContextProviderFilter preAuthnFilter;
    @Autowired
    SpringPostAuthnContextProviderFilter postAuthnFilter;

    private MockMvc mockMvc;

    public static String X_REQUEST_ID_NAME = "x-request-id";
    public static String X_VERSION_NAME = "x-version";
    public static String CUSTOM_NAME = "custom-header";

    public static String ACCEPT_LANGUAGE_VALUE = "ru";
    public static String X_REQUEST_ID_VALUE = "123";
    public static String X_VERSION_VALUE = "v123";
    public static String CUSTOM_VALUE = "value";

    @Autowired
    RestTemplate restTemplate;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).addFilters(preAuthnFilter, postAuthnFilter).build();
    }

    private void sendRequest(String path) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .get(path)
                .header(X_REQUEST_ID_NAME, X_REQUEST_ID_VALUE)
                .header(ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE)
                .header(X_VERSION_NAME, X_VERSION_VALUE)
                .header(CUSTOM_NAME, CUSTOM_VALUE);
        mockMvc.perform(requestBuilder);
    }

    /*
    Test idea: to imitate three microservices. First service send request with some headers (with values for all our
    contexts) to the second. The second service contains restTemplate with filter and interceptor and resend incoming
    request to "/chain_request" endpoint. Third service collects this request and checks that all expected propagated
    headers are present.
     */
    @Test
    @Disabled
    void testRequestPropagation() throws Exception {
        MockRestServiceServer mockServer = MockRestServiceServer.bindTo(restTemplate).build();
        mockServer.expect(requestTo("/chain_request"))
                .andExpect(header(HttpHeaders.ACCEPT_LANGUAGE, ACCEPT_LANGUAGE_VALUE))
                .andExpect(header(X_REQUEST_ID_NAME, X_REQUEST_ID_VALUE))
                .andExpect(header(CUSTOM_NAME, CUSTOM_VALUE))
                .andRespond(withSuccess());

        sendRequest("/test");
    }
}
