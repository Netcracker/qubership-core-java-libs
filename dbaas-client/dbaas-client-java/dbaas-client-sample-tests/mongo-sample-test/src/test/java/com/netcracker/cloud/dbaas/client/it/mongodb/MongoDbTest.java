package com.netcracker.cloud.dbaas.client.it.mongodb;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.config.EnableDbaasMongo;
import org.qubership.cloud.dbaas.client.it.mongodb.access.ServiceDataAccessConfiguration;
import org.qubership.cloud.dbaas.client.it.mongodb.access.TenantDataAccessConfiguration;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;


@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        MongoTestConfiguration.class,
        MongoTestContainerConfiguration.class,
        TestController.class,
        ServiceDataAccessConfiguration.class,
        TenantDataAccessConfiguration.class})
@EnableDbaasMongo
public class MongoDbTest {

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
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
        mockMvc.perform(MockMvcRequestBuilders.delete(TEST_ENDPOINT + "/clear"));
    }

    private void sendRequest(String path, boolean isServiceDb, String id, String name, String tenantName) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(path)
                .param("id", id)
                .param("name", name);
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
    public void testServiceMongoDbCreation() throws Exception {
        UUID testId = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("tenant-for-cleaning"));
        sendRequest(TEST_ENDPOINT + SERVICE_ENDPOINT, true, testId.toString(), "service", null);

        assertEquals(testId.toString(), getContent(TEST_ENDPOINT + SERVICE_ENDPOINT));
    }

    @Test
    public void testTenantMongoDbCreation() throws Exception {
        UUID testId = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant"));
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, testId.toString(), "tenant", "test-tenant");
        assertEquals(testId.toString(), getContent(TEST_ENDPOINT + TENANT_ENDPOINT));
    }

    @Test
    public void testMultitenancy() throws Exception {
        UUID firstTenantId = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant-one"));
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, firstTenantId.toString(), "tenant-one", "test-tenant-one");
        String firstContent = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test-tenant-two"));
        UUID secondTenantId = UUID.randomUUID();
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, secondTenantId.toString(), "tenant-two", "test-tenant-two");
        String secondContent = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        assertNotEquals(firstContent, secondContent);
    }

    @Test
    public void testTenantDbAndServiceDbAreDifferent() throws Exception {
        UUID testServiceId = UUID.randomUUID();
        sendRequest(TEST_ENDPOINT + SERVICE_ENDPOINT, true, testServiceId.toString(), "service", null);
        String firstContent = getContent(TEST_ENDPOINT + SERVICE_ENDPOINT);

        UUID testTenantID = UUID.randomUUID();
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("another-tenant"));
        sendRequest(TEST_ENDPOINT + TENANT_ENDPOINT, false, testTenantID.toString(), "tenant", "another-tenant");
        String tenantContent = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        assertNotEquals(firstContent, tenantContent);
    }

    @Test
    public void testCollectionsInsert() throws Exception {
        UUID testId = UUID.randomUUID();
        sendRequest(TEST_ENDPOINT + "/insert", true, testId.toString(), "service", null);
    }

}
