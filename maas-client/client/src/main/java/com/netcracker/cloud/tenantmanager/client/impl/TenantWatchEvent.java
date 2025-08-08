package com.netcracker.cloud.tenantmanager.client.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.qubership.cloud.tenantmanager.client.Tenant;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class TenantWatchEvent {
	public enum EventType {	SUBSCRIBED, CREATED, MODIFIED, DELETED }

	private EventType type;
	private List<Tenant> tenants = new ArrayList<>();

	private static final ObjectMapper mapper = new ObjectMapper();
	public static TenantWatchEvent deserialize(String json) {
		try {
			return mapper.readValue(json, TenantWatchEvent.class);
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Error deserialize TenantWatchEvent from: " + json, e);
		}
	}
}
