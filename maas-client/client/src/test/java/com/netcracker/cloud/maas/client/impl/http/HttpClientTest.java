package com.netcracker.cloud.maas.client.impl.http;

import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.security.core.utils.k8s.impl.M2MInterceptor;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockserver.integration.ClientAndServer;
import org.mockserver.junit.jupiter.MockServerExtension;

import java.lang.reflect.Field;
import java.util.function.Function;
import java.util.function.Supplier;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockserver.model.HttpRequest.request;
import static org.mockserver.model.HttpResponse.response;

@ExtendWith(MockServerExtension.class)
class HttpClientTest {

    @Test
    void maasClientRebasesRequestsOntoTheConfiguredAgent() throws Exception {
        String agentUrl = "http://maas-agent-custom:9090";
        withProp(Env.PROP_MAAS_AGENT_URL, agentUrl, () -> {
            M2MInterceptor interceptor = m2mInterceptorOf(HttpClient.getMaasClient(() -> "faketoken"));

            assertNotNull(interceptor);
            assertEquals(HttpUrl.get(agentUrl), fallbackBaseUrl(interceptor));
        });
    }

    @Test
    void maasClientFallsBackToTheDefaultAgentAddress() throws Exception {
        withProp(Env.PROP_MAAS_AGENT_URL, null, () -> {
            M2MInterceptor interceptor = m2mInterceptorOf(HttpClient.getMaasClient(() -> "faketoken"));

            assertEquals(HttpUrl.get(Env.DEFAULT_MAAS_AGENT_URL), fallbackBaseUrl(interceptor));
        });
    }

    @Test
    void m2mClientKeepsTheRequestedAddress() throws Exception {
        M2MInterceptor interceptor = m2mInterceptorOf(HttpClient.getM2mClient(() -> "faketoken"));

        assertNotNull(interceptor);
        assertNull(fallbackBaseUrl(interceptor));
    }

    @ParameterizedTest
    @ValueSource(strings = {"maas", "m2m"})
    void customApiAuthSchemeIsSentInsteadOfBearer(String client, ClientAndServer mockServer) {
        mockServer.reset();
        mockServer.when(request().withPath("/api-version").withHeader("Authorization", "Basic dXNlcjpwYXNz"))
                .respond(response().withStatusCode(200).withBody("ok"));

        withProp(Env.PROP_API_AUTH, "Basic", () -> {
            Function<Supplier<String>, HttpClient> factory =
                    client.equals("maas") ? HttpClient::getMaasClient : HttpClient::getM2mClient;
            HttpClient httpClient = factory.apply(() -> "dXNlcjpwYXNz");

            assertNull(m2mInterceptorOf(httpClient));
            String body = httpClient.request("http://localhost:" + mockServer.getPort() + "/api-version")
                    .expect(200)
                    .sendAndReceive(s -> s)
                    .orElse(null);
            assertEquals("ok", body);
        });
    }

    @ParameterizedTest
    @ValueSource(strings = {"Bearer", "bearer", " "})
    void bearerOrBlankApiAuthKeepsM2mClient(String scheme) {
        withProp(Env.PROP_API_AUTH, scheme, () -> assertNotNull(m2mInterceptorOf(HttpClient.getMaasClient(() -> "faketoken"))));
    }

    private M2MInterceptor m2mInterceptorOf(HttpClient httpClient) throws Exception {
        Field field = HttpClient.class.getDeclaredField("httpClient");
        field.setAccessible(true);
        OkHttpClient okHttpClient = (OkHttpClient) field.get(httpClient);
        return (M2MInterceptor) okHttpClient.interceptors().stream()
                .filter(M2MInterceptor.class::isInstance)
                .findFirst()
                .orElse(null);
    }

    private Object fallbackBaseUrl(M2MInterceptor interceptor) throws Exception {
        Field field = M2MInterceptor.class.getDeclaredField("fallbackBaseUrl");
        field.setAccessible(true);
        return field.get(interceptor);
    }
}
