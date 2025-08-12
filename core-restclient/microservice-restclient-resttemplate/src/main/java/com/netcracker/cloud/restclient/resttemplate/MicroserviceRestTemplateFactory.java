package com.netcracker.cloud.restclient.resttemplate;

import com.netcracker.cloud.restclient.MicroserviceRestClient;
import com.netcracker.cloud.restclient.MicroserviceRestClientFactory;

public class MicroserviceRestTemplateFactory implements MicroserviceRestClientFactory {
    @Override
    public MicroserviceRestClient create() {
        return new MicroserviceRestTemplate();
    }
}
