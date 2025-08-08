package com.netcracker.cloud.restclient.resttemplate;

import org.qubership.cloud.restclient.MicroserviceRestClient;
import org.qubership.cloud.restclient.MicroserviceRestClientFactory;

public class MicroserviceRestTemplateFactory implements MicroserviceRestClientFactory {
    @Override
    public MicroserviceRestClient create() {
        return new MicroserviceRestTemplate();
    }
}
