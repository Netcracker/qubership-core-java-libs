package com.netcracker.cloud.restlegacy.restclient.error.v2;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.netcracker.cloud.restlegacy.restclient.error.ErrorsDescription;
import com.netcracker.cloud.restlegacy.restclient.error.ProxyErrorException;
import com.netcracker.cloud.restlegacy.restclient.error.ProxyRethrowException;
import org.springframework.http.HttpStatus;
import org.springframework.retry.RetryContext;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Set;

import static com.netcracker.cloud.restlegacy.restclient.error.v2.ResponseAdviceExceptionHelper.isTransitiveError;


public class RestClientExceptionRetryPolicy extends SimpleRetryPolicy {
    private static final Set<Integer> NOT_RETRYABLE_CODES = new HashSet<>(Arrays.asList(HttpStatus.BAD_REQUEST.value(),
            HttpStatus.UNPROCESSABLE_ENTITY.value(),
            HttpStatus.CONFLICT.value(),
            HttpStatus.NOT_FOUND.value(),
            HttpStatus.UNAUTHORIZED.value(),
            HttpStatus.FORBIDDEN.value()));

    public RestClientExceptionRetryPolicy(int maxRetryAttempts) {
        super(maxRetryAttempts, new HashMap<Class<? extends Throwable>, Boolean>(){{
            put(ProxyRethrowException.class, false);
            put(ProxyErrorException.class, false);
            put(RestClientException.class, false);
        }});
    }

    @Override
    public boolean canRetry(RetryContext context) {

        final Throwable lastThrowable = context.getLastThrowable();
        if (isTransitiveError(lastThrowable)) {
            return false;
        }

        if (lastThrowable instanceof RestClientResponseException) {
            RestClientResponseException restClientResponseException = (RestClientResponseException) lastThrowable;
            if (NOT_RETRYABLE_CODES.contains(restClientResponseException.getStatusCode().value()) || isErrorFromSystemWithLegacyProxyError(restClientResponseException)) {
                return false;
            }
        }

        return super.canRetry(context);
    }

    private static boolean isErrorFromSystemWithLegacyProxyError(RestClientResponseException exception) {
        try {
            ObjectMapper objectMapper = new ObjectMapper();
            ErrorsDescription errorsDescription = objectMapper.readValue(exception.getResponseBodyAsString(), ErrorsDescription.class);
            return errorsDescription.isProxy();
        } catch (Exception ex) {
            return false;
        }
    }
}
