package com.netcracker.cloud.maas.client.impl.dto.kafka.v1;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.netcracker.cloud.maas.client.api.kafka.TopicUserCredentials;
import lombok.Data;
import lombok.ToString;

@Data
public class TopicUserCredentialsImpl implements TopicUserCredentials {
	private String type;
	private String username;
	private String clientKey;
	private String clientCert;
	@JsonProperty("password")
	@ToString.Exclude String encodedPassword;

	@JsonIgnore
	public String getPassword() {
		if (encodedPassword.startsWith("plain:")) {
			return encodedPassword.substring("plain:".length());
		} else {
			throw new IllegalArgumentException("Unsupported password encoding format");
		}
	}
}
