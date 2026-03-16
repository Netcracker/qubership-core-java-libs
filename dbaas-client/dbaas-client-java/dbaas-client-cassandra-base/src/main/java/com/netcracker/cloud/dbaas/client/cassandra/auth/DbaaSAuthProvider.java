package com.netcracker.cloud.dbaas.client.cassandra.auth;

import com.datastax.oss.driver.api.core.auth.PlainTextAuthProviderBase;
import com.datastax.oss.driver.api.core.metadata.EndPoint;
import edu.umd.cs.findbugs.annotations.NonNull;
import lombok.extern.slf4j.Slf4j;

import jakarta.inject.Provider;


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
    protected Credentials getCredentials(@NonNull EndPoint endPoint, @NonNull String serverAuthenticator) {
        return new Credentials(username.toCharArray(), password.toCharArray(), AUTH_ID);
    }
}
