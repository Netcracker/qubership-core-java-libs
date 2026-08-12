package com.netcracker.cloud.security.core.utils.k8s.localdev.impl;

import lombok.Builder;
import lombok.Value;

@Value
@Builder
public class KubeConfigCredentials {
    String serverUrl;
    String userToken;
    byte[] certificateAuthorityData;
    boolean insecureSkipTlsVerify;
}
