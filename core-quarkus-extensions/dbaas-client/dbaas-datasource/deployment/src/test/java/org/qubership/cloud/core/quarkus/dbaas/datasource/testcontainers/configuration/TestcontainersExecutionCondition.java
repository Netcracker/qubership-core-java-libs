package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration;

import org.junit.jupiter.api.extension.ConditionEvaluationResult;
import org.junit.jupiter.api.extension.ExecutionCondition;
import org.junit.jupiter.api.extension.ExtensionContext;


public class TestcontainersExecutionCondition implements ExecutionCondition {
    public static final String SKIP_DOCKER_TESTS_ENV_KEY = "SKIP_TESTCONTAINERS";

    @Override
    public ConditionEvaluationResult evaluateExecutionCondition(ExtensionContext context) {
        boolean skipTestsExecution = Boolean.parseBoolean(System.getenv().getOrDefault(SKIP_DOCKER_TESTS_ENV_KEY, "false"));

        if (skipTestsExecution) {
            return ConditionEvaluationResult.disabled(String.format("Testcontainers execution will be skipped cuz" +
                    " you specified '%s' env variable with 'true' value", SKIP_DOCKER_TESTS_ENV_KEY));
        }
        return ConditionEvaluationResult.enabled("Testcontainers execution will be performed");
    }
}
