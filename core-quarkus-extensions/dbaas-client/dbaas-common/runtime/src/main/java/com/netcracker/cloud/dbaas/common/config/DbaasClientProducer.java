package com.netcracker.cloud.dbaas.common.config;

import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;
import com.netcracker.cloud.dbaas.client.DbaasClient;

@Slf4j
@Singleton
public class DbaasClientProducer {

    @Produces
    @DefaultBean
    public DbaasClient dbaaSClient(DbaasClientConfig dbaasClientConfig, M2MDbaaSClient m2mDbaaSClient) {
        if (dbaasClientConfig.dbaasUrl().isPresent() && dbaasClientConfig.dbaasUsername().isPresent() && dbaasClientConfig.dbaasPassword().isPresent()) {
            log.debug("Create dbaas client with basic auth");
            return new BasicDbaaSClient(dbaasClientConfig).build();
        }

        log.debug("Create dbaas client with m2m auth");
        return m2mDbaaSClient.build();

    }
}
