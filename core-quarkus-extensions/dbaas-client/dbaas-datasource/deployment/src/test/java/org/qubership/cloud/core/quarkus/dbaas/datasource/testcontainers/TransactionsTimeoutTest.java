package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers;

import org.qubership.cloud.framework.contexts.tenant.context.TenantContext;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.PostgresqlContainerResource;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.SkipOnDemand;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.Person;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.service.PersonServiceImpl;
import io.quarkus.test.common.QuarkusTestResource;
import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.configuration.TestUtils.FIRST_TENANT_ID;

@SkipOnDemand
@QuarkusTestResource(PostgresqlContainerResource.class)
@QuarkusTest
@TestProfile(TransactionsTimeoutTest.PostgresqlTimeoutProfile.class)
@Slf4j
public class TransactionsTimeoutTest {

    @Inject
    PersonServiceImpl personService;

    @Test
    public void idleInTransactionTimeoutTest() {
        TenantContext.set(FIRST_TENANT_ID);
        try {
            transactionalTimeoutTest(new Person());
            Assertions.fail();
        } catch (Exception ex) {
            StringBuilder messages = new StringBuilder();
            Throwable throwable = ex;
            while (throwable != null) {
                messages.append(throwable.getMessage());
                throwable = throwable.getCause();
            }
            log.info("got error message: {}", messages);
            Assertions.assertTrue(messages.toString().contains("FATAL: terminating connection due to idle-in-transaction timeout"));
        }
    }

    @Transactional
    public void transactionalTimeoutTest(Person person) throws InterruptedException {
        personService.save(person);
        Thread.sleep(1200);
    }

    @NoArgsConstructor
    public static class PostgresqlTimeoutProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            Map<String, String> properties = new HashMap<>();
            properties.put("quarkus.dbaas.datasource.xa-properties.options", "-c idle-in-transaction-session-timeout=1000");
            return properties;
        }
    }

}


