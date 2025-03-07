package org.qubership.cloud.dbaas.client.entity.connection;


import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@ToString(of = {"url", "username", "role", "tls"})
@AllArgsConstructor
@NoArgsConstructor
public abstract class DatabaseConnection implements AutoCloseable {
    protected String url;
    private String username;
    private transient String password;
    private String role;
    private boolean tls;
    public DatabaseConnection(String url, String username, String password, String role) {
        this.url = url;
        this.username = username;
        this.password = password;
        this.role = role;
    }
}
