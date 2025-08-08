package org.qubership.cloud.tenantmanager.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.tenantmanager.client.Tenant;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class TenantWatchEventTest {
	@Test
	public void testDeserialization() throws JsonProcessingException {
		String json = "{\"type\":\"SUBSCRIBED\",\"tenants\":[]}";
		TenantWatchEvent obj = new ObjectMapper().readValue(json, TenantWatchEvent.class);
		assertEquals(TenantWatchEvent.EventType.SUBSCRIBED, obj.getType());
		assertEquals(0, obj.getTenants().size());
	}

	@Test
	public void testMissingField() throws JsonProcessingException {
		String json = "{\"type\":\"SUBSCRIBED\"}";
		TenantWatchEvent obj = new ObjectMapper().readValue(json, TenantWatchEvent.class);
		assertEquals(TenantWatchEvent.EventType.SUBSCRIBED, obj.getType());
		assertEquals(0, obj.getTenants().size());
	}

	@Test
	public void testNonEmptyMessage() throws JsonProcessingException {
		String json = "{\"type\":\"MODIFIED\",\"tenants\":[{\"objectId\":\"233a1c4c-dde1-4766-9ca5-c0b21400c2b7\",\"externalId\":\"57b5e723-3970-4cde-a5d1-cfee509eecd0\",\"namespace\":null,\"status\":\"ACTIVE\",\"name\":\"user-tenant2\",\"domainName\":null,\"workbook\":null,\"admin\":{\"login\":\"test@example.com\",\"password\":null,\"firstName\":\"Sergey\",\"lastName\":\"Lisovoy\"}}]}";
		TenantWatchEvent obj = new ObjectMapper().readValue(json, TenantWatchEvent.class);
		assertEquals(TenantWatchEvent.EventType.MODIFIED, obj.getType());
		assertEquals(1, obj.getTenants().size());
		Tenant tenant = obj.getTenants().get(0);
		assertEquals("57b5e723-3970-4cde-a5d1-cfee509eecd0", tenant.getExternalId());
		assertEquals("ACTIVE", tenant.getStatus());
		assertEquals("user-tenant2", tenant.getName());
	}
}
