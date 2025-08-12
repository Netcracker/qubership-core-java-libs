package com.netcracker.cloud.restclient.webclient;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.MicroserviceRestClientFactory;

public class MicroserviceWebClientFactory implements MicroserviceRestClientFactory {
    @Override
    public MicroserviceRestClient create() {
        return new MicroserviceWebClient();
    }
}
