package com.netcracker.cloud.maas.client.impl.http;

import com.netcracker.cloud.context.propagation.core.RequestContextPropagation;
import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.security.core.utils.k8s.M2MClientFactory;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.function.Supplier;

public class HttpClient {
    private final OkHttpClient httpClient;

    public static HttpClient getM2mClient(Supplier<String> tokenSupplier) {
        return new HttpClient(M2MClientFactory.getM2MClient(tokenSupplier));
    }

    public static HttpClient getMaasClient(Supplier<String> tokenSupplier) {
        return new HttpClient(M2MClientFactory.getMaasClient(tokenSupplier));
    }

    private HttpClient(OkHttpClient client) {
        this.httpClient = client.newBuilder()
                .addInterceptor(chain -> {
                    Request.Builder reqBuilder = chain.request().newBuilder();

                    // dump context
                    RequestContextPropagation.populateResponse((key, value) -> reqBuilder.header(key, String.valueOf(value)));
                    Env.namespaceOpt().ifPresent(ns -> reqBuilder.header("X-Origin-Namespace", ns));

                    // process request
                    return chain.proceed(reqBuilder.build());
                })
                .readTimeout(Env.httpTimeout())
                .writeTimeout(Env.httpTimeout())
                .connectTimeout(Env.httpTimeout())
                .retryOnConnectionFailure(true)
                .build();
    }

    // it needed for websock connection creation
    public OkHttpClient getClient() {
        return httpClient;
    }

    public HttpExecution request(String url) {
        Request.Builder builder = new Request.Builder();
        builder.url(url);
        return new HttpExecution(httpClient, builder);
    }
}
