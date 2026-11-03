package com.netcracker.cloud.quarkus.security.auth;

import com.netcracker.cloud.security.core.auth.Token;

public class M2MManager implements com.netcracker.cloud.security.core.auth.M2MManager {
    private static M2MManager instance = null;
    private final com.netcracker.cloud.security.core.auth.M2MManager delegate;

    M2MManager() {
        delegate = com.netcracker.cloud.security.core.auth.M2MManager.loadPreferred();
    }

    public static com.netcracker.cloud.security.core.auth.M2MManager getInstance() {
        if (instance == null) {
            synchronized (M2MManager.class) {
                if (instance == null) {
                    instance = new M2MManager();
                }
            }
        }

        return instance;
    }


    @Override
    public Token getToken() {
        return delegate.getToken();
    }
}
