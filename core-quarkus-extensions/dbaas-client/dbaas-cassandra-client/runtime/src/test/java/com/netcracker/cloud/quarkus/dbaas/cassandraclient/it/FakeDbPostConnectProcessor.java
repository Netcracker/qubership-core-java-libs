package com.netcracker.cloud.quarkus.dbaas.cassandraclient.it;

import com.netcracker.cloud.dbaas.common.postprocessor.QuarkusPostConnectProcessor;
import com.netcracker.cloud.quarkus.dbaas.cassandraclient.it.entity.FakeDatabase;
import jakarta.enterprise.context.ApplicationScoped;

@ApplicationScoped
public class FakeDbPostConnectProcessor implements QuarkusPostConnectProcessor<FakeDatabase> {
    @Override
    public void process(FakeDatabase database) {
        throw new RuntimeException("Should never be called");
    }
}
