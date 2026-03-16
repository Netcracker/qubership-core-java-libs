package com.netcracker.cloud.dbaas.client.cassandra.auth;

import com.datastax.oss.driver.api.core.auth.PlainTextAuthProviderBase;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import lombok.extern.slf4j.Slf4j;
import org.jspecify.annotations.NonNull;

@Slf4j
public class DbaaSAuthProvider extends PlainTextAuthProviderBase {
    private static final char[] AUTH_ID = "".toCharArray();

    private String username;
    private String password;

    public DbaaSAuthProvider(String username, String password) {
        super("");
        this.username = username;
        this.password = password;
    }

    @Override
    protected @NonNull Credentials getCredentials(@NonNull EndPoint endPoint, @NonNull String serverAuthenticator) {
        return new Credentials(username.toCharArray(), password.toCharArray(), AUTH_ID);
    }
}
