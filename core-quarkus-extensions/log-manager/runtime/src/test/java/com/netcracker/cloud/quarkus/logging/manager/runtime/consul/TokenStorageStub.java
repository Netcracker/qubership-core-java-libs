package com.netcracker.cloud.quarkus.logging.manager.runtime.consul;

import com.netcracker.cloud.consul.provider.common.TokenStorage;

public class TokenStorageStub implements TokenStorage {
        @Override
        public String get() {
            return "";
        }

        @Override
        public void update(String token) {
            // nothing
        }
    }
