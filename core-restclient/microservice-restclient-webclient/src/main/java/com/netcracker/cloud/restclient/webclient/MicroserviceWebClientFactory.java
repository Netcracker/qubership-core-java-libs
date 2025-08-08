package org.qubership.cloud.restclient.webclient;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.MicroserviceRestClientFactory;

public class MicroserviceWebClientFactory implements MicroserviceRestClientFactory {
    @Override
    public MicroserviceRestClient create() {
        return new MicroserviceWebClient();
    }
}