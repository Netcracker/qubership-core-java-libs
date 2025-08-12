package com.netcracker.cloud.restclient;

public interface MicroserviceRestClientFactory {
    /**
     * Returns default implementation of {@link MicroserviceRestClient}
     */
    MicroserviceRestClient create();
}
