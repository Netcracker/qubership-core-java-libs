package com.netcracker.cloud.security.core.utils.k8s.impl;

import com.netcracker.cloud.security.core.utils.k8s.Priority;
import com.netcracker.cloud.security.core.utils.k8s.TokenSource;
import com.netcracker.cloud.security.core.utils.k8s.localdev.LocalDevMode;
import com.netcracker.cloud.security.core.utils.k8s.localdev.impl.LocalDevTokenSource;
import lombok.extern.slf4j.Slf4j;
import org.jetbrains.annotations.VisibleForTesting;

import java.util.Objects;

@Slf4j
@Priority(0)
public class SelectableTokenSource implements TokenSource {

    private final TokenSource delegate;

    public SelectableTokenSource() {
        this.delegate = LocalDevMode.isEnabled()
                ? new LocalDevTokenSource()
                : new CachingTokenSource();
        if (LocalDevMode.isEnabled()) {
            log.info("Local-dev enabled: using kubeconfig TokenRequest token source");
        }
    }

    @VisibleForTesting
    SelectableTokenSource(TokenSource delegate) {
        this.delegate = Objects.requireNonNull(delegate, "delegate");
    }

    @Override
    public String getToken(String audience) {
        return delegate.getToken(audience);
    }

    @Override
    public void close() throws Exception {
        delegate.close();
    }
}
