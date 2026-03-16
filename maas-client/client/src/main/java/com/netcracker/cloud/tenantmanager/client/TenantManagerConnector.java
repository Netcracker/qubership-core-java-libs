package com.netcracker.cloud.tenantmanager.client;

import java.util.List;
import java.util.function.Consumer;

public interface TenantManagerConnector extends AutoCloseable {
	List<Tenant> getTenantList();

	void subscribe(Consumer<List<Tenant>> callback);
	boolean unsubscribe(Consumer<List<Tenant>> callback);
}
