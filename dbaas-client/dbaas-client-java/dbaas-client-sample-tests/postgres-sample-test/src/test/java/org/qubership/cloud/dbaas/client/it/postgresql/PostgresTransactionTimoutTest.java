package org.qubership.cloud.dbaas.client.it.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.it.postgresql.access.ServiceDataAccessConfiguration;
import org.qubership.cloud.dbaas.client.it.postgresql.access.TenantDataAccessConfiguration;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        TestController.class,
        TestContainerConfiguration.class,
        TestPostgresWithDatasourceConfig.class,
        ServiceDataAccessConfiguration.class,
        TenantDataAccessConfiguration.class,
        DataInitializePostConnectProcessor.class
})
@Slf4j
@TestPropertySource(properties = {
        "dbaas.postgres.datasource.connection-properties=options=-c idle-in-transaction-session-timeout=1000"
})
@EnableTransactionManagement
public class PostgresTransactionTimoutTest {
    @Autowired
    protected WebApplicationContext context;

    private MockMvc mockMvc;

    public static String TEST_SERVICE_ENDPOINT = "/db/microservice/service";

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    private MockHttpServletResponse transactionalRequest(String path, Integer secondsToSleep) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(path)
                .param("milliseconds", secondsToSleep.toString());
        return mockMvc.perform(requestBuilder).andReturn().getResponse();
    }


    @Test
    public void testTransactionalTimeout() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant_for_cleaning"));
        try {
            assertEquals(transactionalRequest(TEST_SERVICE_ENDPOINT + "/transactional", 1200).getStatus(), 200);
            Assertions.fail();
        } catch (Exception ex){
            assertEquals("FATAL: terminating connection due to idle-in-transaction timeout", ex.getCause().getCause().getCause().getMessage());
        }
    }
}
