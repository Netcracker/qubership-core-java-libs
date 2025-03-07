package org.qubership.cloud.consul.provider.common.client;

import com.google.gson.Gson;
import okhttp3.*;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.consul.provider.common.client.ConsulClient.*;
import static org.junit.jupiter.api.Assertions.assertEquals;
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
}
