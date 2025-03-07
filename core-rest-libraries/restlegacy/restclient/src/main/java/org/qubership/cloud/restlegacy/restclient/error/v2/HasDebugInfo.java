package org.qubership.cloud.restlegacy.restclient.error.v2;

public interface HasDebugInfo<Info extends DebugInfo> {
    Info getDebugInfo();

    void setDebugInfo(Info debugInfo);
}
