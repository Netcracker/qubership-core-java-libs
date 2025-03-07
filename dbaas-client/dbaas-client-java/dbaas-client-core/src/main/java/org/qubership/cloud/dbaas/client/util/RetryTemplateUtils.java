package org.qubership.cloud.dbaas.client.util;

import org.qubership.cloud.dbaas.client.exceptions.DatabaseNotReadyException;

import org.springframework.classify.BinaryExceptionClassifier;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.BinaryExceptionClassifierRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

public class RetryTemplateUtils {

    public static RetryTemplate createAwaitAsyncDbCreationRetryTemplate(int timeoutSeconds) {
        RetryTemplate asyncRetryTemplate = new RetryTemplate();
        RetryPolicy timeoutPolicy = new TimeoutRetryPolicy(timeoutSeconds * 1000L);

        RetryPolicy asyncCheckPolicy = new BinaryExceptionClassifierRetryPolicy(
                BinaryExceptionClassifier.builder().retryOn(DatabaseNotReadyException.class).build()
        );

        CompositeRetryPolicy compositePolicy = new CompositeRetryPolicy();
        compositePolicy.setPolicies(new RetryPolicy[]{timeoutPolicy, asyncCheckPolicy});
        asyncRetryTemplate.setRetryPolicy(compositePolicy);

        ExponentialBackOffPolicy backOffPolicy = new ExponentialBackOffPolicy();
        backOffPolicy.setInitialInterval(1000L);
        backOffPolicy.setMultiplier(2);
        backOffPolicy.setMaxInterval(60000L);
        asyncRetryTemplate.setBackOffPolicy(backOffPolicy);

        return asyncRetryTemplate;
    }

}
