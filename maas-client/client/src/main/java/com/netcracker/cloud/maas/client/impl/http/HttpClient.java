package com.netcracker.cloud.maas.client.impl.http;

import org.qubership.cloud.context.propagation.core.RequestContextPropagation;
import org.qubership.cloud.maas.client.impl.Env;
import okhttp3.OkHttpClient;
import okhttp3.Request;

import java.util.function.Supplier;

public class HttpClient {
    private final OkHttpClient httpClient;

    public HttpClient(Supplier<String> tokenSupplier) {
        this.httpClient = new OkHttpClient.Builder()
                .addInterceptor(chain -> {
                    Request.Builder reqBuilder = chain.request().newBuilder();

                    // dump context
                    RequestContextPropagation.populateResponse((key, value) -> reqBuilder.header(key, String.valueOf(value)));

                    // add authorization token
                    reqBuilder.header("Authorization", Env.apiAuth() + " " + tokenSupplier.get());
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
