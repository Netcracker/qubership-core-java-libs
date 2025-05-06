package org.qubership.cloud.dbaas.client.it.cassandra;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.config.EnableDbaasCassandra;
import org.qubership.cloud.dbaas.client.it.cassandra.config.CassandraTestConfiguration;
import org.qubership.cloud.dbaas.client.it.cassandra.config.DataInitializePostConnectProcessor;
import org.qubership.cloud.dbaas.client.it.cassandra.config.ServiceDataAccessConfiguration;
import org.qubership.cloud.dbaas.client.it.cassandra.config.TenantDataAccessConfiguration;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;


@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Import({DataInitializePostConnectProcessor.class})
@ContextConfiguration(classes = {
        CassandraTestConfiguration.class,
        TestController.class,
        ServiceDataAccessConfiguration.class,
        TenantDataAccessConfiguration.class})
@EnableDbaasCassandra
public class CassandraTest {

    @Autowired
    protected WebApplicationContext context;

    private MockMvc mockMvc;

    public static String TEST_ENDPOINT = "/db/microservice";
    public static String SERVICE_ENDPOINT = "/service";
    public static String TENANT_ENDPOINT = "/tenant";

    @BeforeEach
    public void setUp() {
        mockMvc = MockMvcBuilders.webAppContextSetup(context).build();
    }

    @AfterEach
    public void clean() throws Exception {
        mockMvc.perform(MockMvcRequestBuilders.delete(TEST_ENDPOINT + "/clear"));
    }

    private void sendRequest(String path, boolean isServiceDb, String id, String name, String tenantName) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(path)
                .param("id", id)
                .param("title", name)
                .param("year", "1994");
        if (!isServiceDb) {
            requestBuilder.header("tenant", tenantName);
        }
        mockMvc.perform(requestBuilder);
    }

    private String getContent(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testServiceCassandraDbCreation() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test"));
        UUID testId = UUID.randomUUID();
        sendRequest(TEST_ENDPOINT + SERVICE_ENDPOINT, true, testId.toString(), "service", null);

        assertEquals(testId.toString(), getContent(TEST_ENDPOINT + SERVICE_ENDPOINT));
    }

    @Test
    public void testTenantCassandraDbCreation() throws Exception {
        UUID testId = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant"));
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, testId.toString(), "tenant", "test_tenant");
        assertEquals(testId.toString(), getContent(TEST_ENDPOINT + TENANT_ENDPOINT));
    }

    @Test
    public void testMultitenancy() throws Exception {
        UUID firstTenantId = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant_one"));
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, firstTenantId.toString(), "tenant-one", "test_tenant_one");
        String firstContent = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant_two"));
        UUID secondTenantId = UUID.randomUUID();
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, secondTenantId.toString(), "tenant-two", "test_tenant_two");
        String secondContent = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        assertNotEquals(firstContent, secondContent);
    }

    @Test
    public void testTenantDbAndServiceDbAreDifferent() throws Exception {
        UUID testServiceId = UUID.randomUUID();
        sendRequest(TEST_ENDPOINT + SERVICE_ENDPOINT, true, testServiceId.toString(), "service", null);
        String firstContent = getContent(TEST_ENDPOINT + SERVICE_ENDPOINT);

        UUID testTenantID = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("another_tenant"));
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, testTenantID.toString(), "tenant", "another_tenant");
        String tenantContent = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        assertNotEquals(firstContent, tenantContent);
    }
}
