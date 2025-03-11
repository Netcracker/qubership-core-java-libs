package org.qubership.cloud.dbaas.common.config;

import org.qubership.cloud.dbaas.common.classifier.DbaaSClassifierFactory;
import io.quarkus.arc.DefaultBean;
import jakarta.enterprise.inject.Produces;
import jakarta.inject.Singleton;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Singleton
public class DbaaSClassifierProducer {
    @Produces
    @DefaultBean
    public DbaaSClassifierFactory createClassifierFactory() {
        return new DbaaSClassifierFactory();
    }

}
