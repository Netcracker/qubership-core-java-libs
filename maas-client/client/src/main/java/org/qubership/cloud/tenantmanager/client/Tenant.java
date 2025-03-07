package org.qubership.cloud.tenantmanager.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.With;

import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Tenant {
	public static final String STATE_ACTIVE = "ACTIVE";
	private String externalId;
	private String name;
	private String namespace;
	@With private String status;
}
