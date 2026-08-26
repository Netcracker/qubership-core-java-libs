package com.netcracker.cloud.tenantmanager.client.impl;

import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.tenantmanager.client.Tenant;
import com.netcracker.cloud.testharness.MaaSCocoonExtension;
import com.netcracker.cloud.testharness.TenantManagerMockInject;
import com.netcracker.cloud.testharness.TenantManagerMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;

import java.util.List;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;
import java.util.concurrent.TimeUnit;

import static com.netcracker.cloud.maas.client.Utils.withProp;
import static org.junit.jupiter.api.Assertions.*;


@ExtendWith(MaaSCocoonExtension.class)
@Slf4j
class TenantManagerConnectorImplTest {

    @TenantManagerMockInject
    TenantManagerMockServer tmMock;

    private static final long EVENT_TIMEOUT_SEC = 10;
    private static final long NO_EVENT_TIMEOUT_SEC = 1;

    @Test
    public void testApi() throws Exception {
        BlockingQueue<List<Tenant>> events = new LinkedBlockingDeque<>();
        try (TenantManagerConnectorImpl client = new TenantManagerConnectorImpl(tmMock.getUrl(), HttpClient.getM2mClient(() -> "faketoken"))) {
            client.subscribe(events::add);
            List<Tenant> tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(0, tenants.size());

            String firstId = tmMock.addFirstActivatedTenant();
            tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(1, tenants.size());
            assertEquals(firstId, tenants.get(0).getExternalId());

            String secondId = tmMock.addSecondActivatedTenant();
            tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(2, tenants.size());
            assertArrayEquals(new String[]{firstId, secondId}, tenants.stream().map(Tenant::getExternalId).toArray());

            tmMock.deactivateSecondTenant();
            tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(1, tenants.size());
            assertEquals(firstId, tenants.get(0).getExternalId());

            tmMock.deleteFirstTenant();
            tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(0, tenants.size());
        }
    }

    @Test
    public void testReconnect() throws Exception {
        withProp(Env.PROP_TENANT_MANAGER_RECONNECT_TIMEOUT, "PT1S", () -> {
            BlockingQueue<List<Tenant>> events = new LinkedBlockingDeque<>();
            try (TenantManagerConnectorImpl client = new TenantManagerConnectorImpl(tmMock.getUrl(), HttpClient.getM2mClient(() -> "faketoken"))) {

                client.subscribe(events::add);
                List<Tenant> tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
                assertNotNull(tenants);
                assertEquals(0, tenants.size());

                String firstId = tmMock.addFirstActivatedTenant();
                tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
                assertNotNull(tenants);
                assertEquals(1, tenants.size());
                assertEquals(firstId, tenants.get(0).getExternalId());

                // emulate tenant-manager service unexpected restart
                tmMock.stop();
                tmMock.start();

                Thread.sleep(2_000); // reconnect timeout above is PT1S, give the reconnect time to fire

                // tenant should be cache from previous connection session
                assertEquals(1, client.getTenantList().size());

                tenants = events.poll(NO_EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
                assertNull(tenants); // no messages should be send due of reconnection

                // check that tenant list change is processed normally
                tmMock.addSecondActivatedTenant();
                tenants = events.poll(EVENT_TIMEOUT_SEC, TimeUnit.SECONDS);
                assertNotNull(tenants, "no tenant list update arrived after the reconnect");
                assertEquals(2, tenants.size());
                assertEquals(2, client.getTenantList().size());
            }
        });
    }
}
