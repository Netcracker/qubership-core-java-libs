package com.netcracker.cloud.tenantmanager.client.impl;

import com.netcracker.cloud.maas.client.impl.Env;
import com.netcracker.cloud.maas.client.impl.http.HttpClient;
import com.netcracker.cloud.tenantmanager.client.Tenant;
import com.netcracker.cloud.testharness.MaaSCocoonExtension;
import com.netcracker.cloud.testharness.TenantManagerMockInject;
import com.netcracker.cloud.testharness.TenantManagerMockServer;
import lombok.extern.slf4j.Slf4j;
import org.junit.jupiter.api.Disabled;
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
@Disabled // TODO fix this problem
class TenantManagerConnectorImplTest {

    @TenantManagerMockInject
    TenantManagerMockServer tmMock;

    @Test
    public void testApi() throws Exception {
        BlockingQueue<List<Tenant>> events = new LinkedBlockingDeque<>();
        try (TenantManagerConnectorImpl client = new TenantManagerConnectorImpl(tmMock.getUrl(), new HttpClient(() -> "faketoken"))) {
            client.subscribe(events::add);
            List<Tenant> tenants = events.poll(1, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(0, tenants.size());

            String firstId = tmMock.addFirstActivatedTenant();
            tenants = events.poll(1, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(1, tenants.size());
            assertEquals(firstId, tenants.get(0).getExternalId());

            String secondId = tmMock.addSecondActivatedTenant();
            tenants = events.poll(1, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(2, tenants.size());
            assertArrayEquals(new String[]{firstId, secondId}, tenants.stream().map(Tenant::getExternalId).toArray());

            tmMock.deactivateSecondTenant();
            tenants = events.poll(1, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(1, tenants.size());
            assertEquals(firstId, tenants.get(0).getExternalId());

            tmMock.deleteFirstTenant();
            tenants = events.poll(1, TimeUnit.SECONDS);
            assertNotNull(tenants);
            assertEquals(0, tenants.size());
        }
    }

    @Test
    public void testReconnect() throws Exception {
        withProp(Env.PROP_TENANT_MANAGER_RECONNECT_TIMEOUT, "1", () -> {
            BlockingQueue<List<Tenant>> events = new LinkedBlockingDeque<>();
            try (TenantManagerConnectorImpl client = new TenantManagerConnectorImpl(tmMock.getUrl(), new HttpClient(() -> "faketoken"))) {

                client.subscribe(events::add);
                List<Tenant> tenants = events.poll(1, TimeUnit.SECONDS);
                assertNotNull(tenants);
                assertEquals(0, tenants.size());

                String firstId = tmMock.addFirstActivatedTenant();
                tenants = events.poll(1, TimeUnit.SECONDS);
                assertNotNull(tenants);
                assertEquals(1, tenants.size());
                assertEquals(firstId, tenants.get(0).getExternalId());

                // emulate tenant-manager service unexpected restart
                tmMock.stop();
                tmMock.start();

                Thread.sleep(2); // wait PROP_RECONNECT_TIMEOUT

                // tenant should be cache from previous connection session
                assertEquals(1, client.getTenantList().size());

                tenants = events.poll(1, TimeUnit.SECONDS);
                assertNull(tenants); // no messages should be send due of reconnection

                // check that tenant list change is processed normally
                tmMock.addSecondActivatedTenant();
                tenants = events.poll(2, TimeUnit.SECONDS);
                assertEquals(2, tenants.size());
                assertEquals(2, client.getTenantList().size());
            }
        });
    }
}
