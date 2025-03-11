package org.qubership.cloud.dbaas.common.config;
import okhttp3.Interceptor;
import okhttp3.Response;
import org.jboss.logging.Logger;

import java.io.IOException;

public class RetryInterceptor implements Interceptor {
    private static final Logger log = Logger.getLogger(RetryInterceptor.class);

    private int maxRetries;
    private long initialDelayMillis;

    public RetryInterceptor(int maxRetries, long initialDelayMillis) {
        this.maxRetries = maxRetries;
        this.initialDelayMillis = initialDelayMillis;
    }

    @Override
    public Response intercept(Chain chain) throws IOException {
        int retryCount = 0;
        long retryDelay = initialDelayMillis; // Initial retry delay in milliseconds
        while (true) {
            try {
                return chain.proceed(chain.request());
            } catch (IOException e) {
                if (e instanceof java.net.SocketTimeoutException && retryCount < maxRetries) {
                    retryCount++;
                    try {
                        Thread.sleep(retryDelay);
                        retryDelay *= 2; // Exponential backoff
                    } catch (InterruptedException ex) {
                        log.warn("Retry sleep interrupted", ex);
                        Thread.currentThread().interrupt();
                    }
                } else {
                    log.error("IOException encountered: " + e.getMessage(), e);
                    throw e;
                }
            }
        }
    }
}
