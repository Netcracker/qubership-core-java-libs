package org.qubership.cloud.restclient;

public interface MicroserviceRestClientFactory {
    /**
     * Returns default implementation of {@link MicroserviceRestClient}
     */
    MicroserviceRestClient create();
}
