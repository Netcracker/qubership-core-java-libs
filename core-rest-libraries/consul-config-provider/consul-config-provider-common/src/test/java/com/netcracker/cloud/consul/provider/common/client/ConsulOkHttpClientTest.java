package com.netcracker.cloud.consul.provider.common.client;

import com.google.gson.Gson;
import com.netcracker.cloud.consul.provider.common.ConsulLoginCredentials;
import okhttp3.*;
import okio.Buffer;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static com.netcracker.cloud.consul.provider.common.client.ConsulClient.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;


class ConsulOkHttpClientTest {

    private static final Logger LOGGER = LoggerFactory.getLogger(ConsulOkHttpClientTest.class);
    public static final String TEXT = "success";
    public static final int SUCCESS_CODE = 200;
    public static final String CONSUL_ADDRESS = "http://consul:8301";

    @Test
    void getSelfTokenSuccessTest() {
        String currentSecretId = "my-current-secret-id";

        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        Call call = mock(Call.class);

        try {
            Request okHttpRequest = new Request.Builder().get()
                    .url(CONSUL_ADDRESS + V1_ACL_TOKEN_SELF)
                    .addHeader(X_CONSUL_TOKEN_HEADER, currentSecretId).build();
            Response okHttpResponse = new Response.Builder().request(okHttpRequest)
                    .protocol(Protocol.HTTP_2).message(TEXT)
                    .code(SUCCESS_CODE).body(responseBody).build();

            when(responseBody.string()).thenReturn(TEXT);
            when(okHttpClient.newCall(any())).thenReturn(call);
            when(call.execute()).thenReturn(okHttpResponse);

            ConsulClient consulClient = new ConsulOkHttpClient(okHttpClient, CONSUL_ADDRESS, () -> "");
            ConsulClientResponse consulClientResponse = consulClient.getSelfToken(currentSecretId);
            Mockito.verify(okHttpClient, Mockito.times(1))
                    .newCall(argThat(arg ->
                            arg.url().toString().equals(okHttpRequest.url().toString()) &&
                                    arg.method().equals("GET") &&
                                    arg.header(X_CONSUL_TOKEN_HEADER).equals(currentSecretId)));
            assertEquals(SUCCESS_CODE, consulClientResponse.getCode());
            assertEquals(TEXT, consulClientResponse.getBodyJson());
        } catch (IOException e) {
            LOGGER.error("Failed to run getSelfTokenSuccessTest ", e);
            fail();
        }
    }

    @Test
    void loginSuccessTest() {
        String authMethod = "core-ci";
        String m2m = "my-secret-m2m-token";

        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        Call call = mock(Call.class);
        Map<String, String> payload = new HashMap<>();
        payload.put(AUTH_METHOD_FIELD, authMethod);
        payload.put(BEARER_TOKEN_FIELD, m2m);
        String jsonPayload = new Gson().toJson(payload);

        try {
            Request okHttpRequest = new Request.Builder()
                    .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), jsonPayload))
                    .url(CONSUL_ADDRESS + V1_ACL_LOGIN)
                    .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                    .build();
            Response okHttpResponse = new Response.Builder().request(okHttpRequest)
                    .protocol(Protocol.HTTP_2).message(TEXT)
                    .code(SUCCESS_CODE).body(responseBody).build();

            when(responseBody.string()).thenReturn(TEXT);
            when(okHttpClient.newCall(any())).thenReturn(call);
            when(call.execute()).thenReturn(okHttpResponse);

            ConsulClient consulClient = new ConsulOkHttpClient(okHttpClient, CONSUL_ADDRESS, () -> m2m);
            ConsulClientResponse consulResponse = consulClient.login(authMethod);
            Mockito.verify(okHttpClient, Mockito.times(1))
                    .newCall(argThat(arg ->
                            arg.url().toString().equals(okHttpRequest.url().toString()) &&
                                    arg.method().equals("POST") &&
                                    arg.header(CONTENT_TYPE).equals(APPLICATION_JSON)));
            assertEquals(SUCCESS_CODE, consulResponse.getCode());
            assertEquals(TEXT, consulResponse.getBodyJson());
        } catch (IOException e) {
            LOGGER.error("Failed to run loginSuccessTest", e);
            fail();
        }
    }

    private static ConsulLoginCredentials credentials(String authMethod, String bearerToken) {
        return new ConsulLoginCredentials() {
            @Override
            public String authMethod() {
                return authMethod;
            }

            @Override
            public String bearerToken() {
                return bearerToken;
            }
        };
    }

    private static String bodyOf(Request request) throws IOException {
        Buffer buffer = new Buffer();
        request.body().writeTo(buffer);
        return buffer.readUtf8();
    }

    @Test
    void loginByCredentialsFailsWithIOExceptionOnBrokenConnection() throws IOException {
        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        Call call = mock(Call.class);

        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenThrow(new IOException("connection reset"));

        ConsulClient consulClient = new ConsulOkHttpClient(okHttpClient, CONSUL_ADDRESS, () -> "");

        IOException thrown = assertThrows(IOException.class,
                () -> consulClient.login(credentials("core-k8s", "my-secret-bearer-token")));
        assertEquals("connection reset", thrown.getMessage());
    }

    @Test
    void loginByCredentialsSendsAuthMethodAndBearerTokenFromCredentials() throws IOException {
        String authMethod = "core-k8s";
        String bearerToken = "my-secret-bearer-token";

        OkHttpClient okHttpClient = mock(OkHttpClient.class);
        ResponseBody responseBody = mock(ResponseBody.class);
        Call call = mock(Call.class);

        Request okHttpRequest = new Request.Builder()
                .post(RequestBody.create(MediaType.parse(APPLICATION_JSON), "{}"))
                .url(CONSUL_ADDRESS + V1_ACL_LOGIN)
                .addHeader(CONTENT_TYPE, APPLICATION_JSON)
                .build();
        Response okHttpResponse = new Response.Builder().request(okHttpRequest)
                .protocol(Protocol.HTTP_2).message(TEXT)
                .code(SUCCESS_CODE).body(responseBody).build();

        when(responseBody.string()).thenReturn(TEXT);
        when(okHttpClient.newCall(any())).thenReturn(call);
        when(call.execute()).thenReturn(okHttpResponse);

        ConsulClient consulClient = new ConsulOkHttpClient(okHttpClient, CONSUL_ADDRESS, () -> "unused-m2m-token");
        ConsulClientResponse consulResponse = consulClient.login(credentials(authMethod, bearerToken));

        ArgumentCaptor<Request> sent = ArgumentCaptor.forClass(Request.class);
        Mockito.verify(okHttpClient, Mockito.times(1)).newCall(sent.capture());

        Map<String, String> body = new Gson().fromJson(bodyOf(sent.getValue()), Map.class);
        assertEquals(authMethod, body.get(AUTH_METHOD_FIELD));
        assertEquals(bearerToken, body.get(BEARER_TOKEN_FIELD));
        assertEquals(CONSUL_ADDRESS + V1_ACL_LOGIN, sent.getValue().url().toString());
        assertEquals("POST", sent.getValue().method());
        assertTrue(APPLICATION_JSON.equals(sent.getValue().header(CONTENT_TYPE)));
        assertEquals(SUCCESS_CODE, consulResponse.getCode());
        assertEquals(TEXT, consulResponse.getBodyJson());
    }
}
