package org.qubership.cloud.restlegacy.restclient.retry;

import org.qubership.cloud.restlegacy.restclient.error.v2.RestClientExceptionRetryPolicy;
import org.springframework.retry.RetryPolicy;
import org.springframework.retry.backoff.FixedBackOffPolicy;
import org.springframework.retry.policy.CircuitBreakerRetryPolicy;
import org.springframework.retry.policy.CompositeRetryPolicy;
import org.springframework.retry.policy.NeverRetryPolicy;
import org.springframework.retry.policy.TimeoutRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.retry.policy.SimpleRetryPolicy.DEFAULT_MAX_ATTEMPTS;

public class RetryTemplateBuilder {

    private boolean useSimpleRetryPolicy;
    private Integer maxRetryAttempts;

    private boolean useTimeoutRetryPolicy;
    private Long operationsTimeOut;

    private boolean useBackoffPolicy;
    private Long backOffPeriod;

    private boolean useCircuitBreaker;
    private Long circuitBreakerOpenTimeout;
    private Long circuitBreakerResetTimeout;

    private boolean useNeverRetryPolicy;

    public RetryTemplateBuilder() {
    }

    public RetryTemplateBuilder withSimpleRetryPolicy() {
        this.useSimpleRetryPolicy = true;
        return this;
    }

    public RetryTemplateBuilder withTimeoutRetryPolicy() {
        this.useTimeoutRetryPolicy = true;
        return this;
    }

    public RetryTemplateBuilder withCircuitBreaker() {
        this.useCircuitBreaker = true;
        return this;
    }

    public RetryTemplateBuilder withNeverRetryPolicy() {
        this.useNeverRetryPolicy = true;
        return this;
    }

    public RetryTemplateBuilder withBackOff() {
        this.useBackoffPolicy = true;
        return this;
    }

    public RetryTemplateBuilder withMaxRetryAttempts(Integer maxRetryAttempts) {
        this.maxRetryAttempts = maxRetryAttempts;
        return this;
    }

    public RetryTemplateBuilder withOperationsTimeOutInMillis(Long operationsTimeOutInMillis) {
        this.operationsTimeOut = operationsTimeOutInMillis;
        return this;
    }

    public RetryTemplateBuilder withBackOffPeriodInMillis(Long backOffPeriodInMillis) {
        this.backOffPeriod = backOffPeriodInMillis;
        return this;
    }

    public RetryTemplateBuilder withCircuitBreakerOpenTimeoutInMillis(Long circuitBreakerOpenTimeoutInMillis) {
        this.circuitBreakerOpenTimeout = circuitBreakerOpenTimeoutInMillis;
        return this;
    }

    public RetryTemplateBuilder withCircuitBreakerResetTimeoutInMillis(Long circuitBreakerResetTimeoutInMillis) {
        this.circuitBreakerResetTimeout = circuitBreakerResetTimeoutInMillis;
        return this;
    }

    public RetryTemplate build() {

        if (!useSimpleRetryPolicy && !useTimeoutRetryPolicy && !useNeverRetryPolicy) {
            //pessimistic composite retry policy will produce endless retry loop if its contents are empty
            throw new IllegalStateException("Cannot build RetryTemplate without at least one basic retry policy");
        }

        final RetryTemplate retryTemplate = new RetryTemplate();
        final List<RetryPolicy> retryPolicies = new ArrayList<>();

        if (useSimpleRetryPolicy) {
            if (maxRetryAttempts == null) {
                maxRetryAttempts = DEFAULT_MAX_ATTEMPTS;
            }
            retryPolicies.add(new RestClientExceptionRetryPolicy(maxRetryAttempts));
        }

        if (useTimeoutRetryPolicy) {
            final TimeoutRetryPolicy timeoutRetryPolicy = new TimeoutRetryPolicy();
            if (operationsTimeOut != null) {
                timeoutRetryPolicy.setTimeout(operationsTimeOut);
            }
            retryPolicies.add(timeoutRetryPolicy);
        }

        if (useNeverRetryPolicy) {
            final NeverRetryPolicy neverRetryPolicy = new NeverRetryPolicy();
            retryPolicies.add(neverRetryPolicy);
        }

        //Compose policies above into one
        final CompositeRetryPolicy compositeRetryPolicy = new CompositeRetryPolicy();
        compositeRetryPolicy.setOptimistic(false); //fail when either of underlying policies fail
        compositeRetryPolicy.setPolicies(retryPolicies.toArray(new RetryPolicy[retryPolicies.size()]));

        retryTemplate.setRetryPolicy(compositeRetryPolicy);

        //Front everything with circuit breaker if needed
        if (useCircuitBreaker) {
            final CircuitBreakerRetryPolicy circuitBreakerRetryPolicy = new CircuitBreakerRetryPolicy(compositeRetryPolicy);
            if (circuitBreakerOpenTimeout != null) {
                circuitBreakerRetryPolicy.setOpenTimeout(circuitBreakerOpenTimeout);
            }
            if (circuitBreakerResetTimeout != null) {
                circuitBreakerRetryPolicy.setResetTimeout(circuitBreakerResetTimeout);
            }
            retryTemplate.setRetryPolicy(circuitBreakerRetryPolicy);
        }

        if (useBackoffPolicy) {
            final FixedBackOffPolicy backOffPolicy = new FixedBackOffPolicy();
            if (backOffPeriod != null) {
                backOffPolicy.setBackOffPeriod(backOffPeriod);
            }
            retryTemplate.setBackOffPolicy(backOffPolicy);
        }

        return retryTemplate;
    }
}