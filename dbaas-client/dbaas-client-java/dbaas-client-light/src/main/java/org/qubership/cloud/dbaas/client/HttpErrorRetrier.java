package org.qubership.cloud.dbaas.client;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import okhttp3.Interceptor;
import okhttp3.Request;
import okhttp3.Response;

import java.io.IOException;
import java.util.List;

@Slf4j
@RequiredArgsConstructor
public class HttpErrorRetrier implements Interceptor {
    @NonNull
    private List<Integer> codes;
    @NonNull
    private Integer limit;
    @NonNull
    private Integer sleepMultiplier;


    @Override
    public Response intercept(Chain chain) throws IOException {
        Request request = chain.request();
        Response response = chain.proceed(request);
        int tryCount = 0;
        int sleepSeconds = 1;
        while (codes.contains(response.code()) && tryCount++ < limit) {
            log.info("Request to {} was not successful with code {}, retry {} of {}",
                    request.url(), response.code(), tryCount, limit);
            try {
                Thread.sleep((long)sleepSeconds *  1000);
            } catch (InterruptedException e) {
                log.error("Retry of {} was interrupted", request.url());
                return response;
            }
            sleepSeconds = sleepMultiplier * sleepSeconds;
            response = chain.proceed(request);
        }
        return response;
    }
}
