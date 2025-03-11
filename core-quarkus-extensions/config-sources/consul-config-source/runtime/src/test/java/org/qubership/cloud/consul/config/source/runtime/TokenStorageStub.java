package org.qubership.cloud.consul.config.source.runtime;

import org.qubership.cloud.consul.provider.common.TokenStorage;

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