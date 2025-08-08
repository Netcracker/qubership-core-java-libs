package org.qubership.cloud.maas.client.api.rabbit;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.ToString;

import java.net.URI;

@Data
public class VHost {
    String cnn;
    String username;
    String apiUrl;

    @JsonProperty("password")
    @ToString.Exclude String encodedPassword;

    @JsonIgnore
    public URI getUri() {
        return URI.create(cnn);
    }

    @JsonIgnore
    public String getPassword() {
        if (encodedPassword.startsWith("plain:")) {
            return encodedPassword.substring("plain:".length());
        } else {
            throw new IllegalArgumentException("Unsupported password encoding format");
        }
    }
}
