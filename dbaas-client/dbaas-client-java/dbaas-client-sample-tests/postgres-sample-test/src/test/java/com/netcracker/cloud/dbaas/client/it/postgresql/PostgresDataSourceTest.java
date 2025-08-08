package com.netcracker.cloud.dbaas.client.it.postgresql;

import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import com.netcracker.cloud.context.propagation.core.ContextManager;
import com.netcracker.cloud.dbaas.client.it.postgresql.access.ServiceDataAccessConfiguration;
import com.netcracker.cloud.dbaas.client.it.postgresql.access.TenantDataAccessConfiguration;
import com.netcracker.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static com.netcracker.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@ContextConfiguration(classes = {
        TestController.class,
        TestPostgresWithDatasourceConfig.class,
        TestContainerConfiguration.class,
        ServiceDataAccessConfiguration.class,
        TenantDataAccessConfiguration.class,
        DataInitializePostConnectProcessor.class
})
@Slf4j
public class PostgresDataSourceTest {
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

    private String sendRequestAndGetId(String path, boolean isServiceDb, String fName, String lName, String tenantName) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(path)
                .param("fName", fName)
                .param("lName", lName);
        if (!isServiceDb) {
            requestBuilder.header("tenant", tenantName);
        }
        return mockMvc.perform(requestBuilder).andReturn().getResponse().getContentAsString();
    }

    private String getNameById(String path, String id) throws Exception {
        MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders
                .post(path)
                .param("id", id);
        return mockMvc.perform(requestBuilder).andReturn().getResponse().getContentAsString();
    }

    private String getContent(String path) throws Exception {
        return mockMvc.perform(MockMvcRequestBuilders.get(path))
                .andReturn().getResponse().getContentAsString();
    }

    @Test
    public void testServiceDataSourceCreation() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant_for_cleaning"));
        String createdPersonId = sendRequestAndGetId(TEST_ENDPOINT + SERVICE_ENDPOINT, true, "name", "surname", null);
        String getPersonFirstName = getContent(TEST_ENDPOINT + SERVICE_ENDPOINT);
        assertEquals(getNameById(TEST_ENDPOINT + SERVICE_ENDPOINT + "Id", createdPersonId), getPersonFirstName);
    }

    @Test
    public void testTenantDataSourceCreation() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant"));
        String createdPersonId = sendRequestAndGetId(TEST_ENDPOINT + TENANT_ENDPOINT, false, "name", "surname", "test_tenant");
        String gotPersonFirstName = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);
        assertEquals(getNameById(TEST_ENDPOINT + TENANT_ENDPOINT + "Id", createdPersonId), gotPersonFirstName);
    }

    @Test
    public void testMultitenancy() throws Exception {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant_one"));
        sendRequestAndGetId(TEST_ENDPOINT + TENANT_ENDPOINT, false, "name", "surname", "test_tenant_one");
        String firstTenantName = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant_two"));
        sendRequestAndGetId(TEST_ENDPOINT + TENANT_ENDPOINT, false, "secName", "secSurname", "test_tenant_two");
        String secondTenantName = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        assertNotEquals(firstTenantName, secondTenantName);
    }

    @Test
    public void testTenantDbAndServiceDbAreDifferent() throws Exception {

        sendRequestAndGetId(TEST_ENDPOINT + SERVICE_ENDPOINT, true, "name", "surname", null);
        String personServiceName = getContent(TEST_ENDPOINT + SERVICE_ENDPOINT);

        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject("test_tenant"));
        sendRequestAndGetId(TEST_ENDPOINT + TENANT_ENDPOINT, false, "secName", "secSurname", "test_tenant");
        String personTenantName = getContent(TEST_ENDPOINT + TENANT_ENDPOINT);

        assertNotEquals(personServiceName, personTenantName);
    }
}
