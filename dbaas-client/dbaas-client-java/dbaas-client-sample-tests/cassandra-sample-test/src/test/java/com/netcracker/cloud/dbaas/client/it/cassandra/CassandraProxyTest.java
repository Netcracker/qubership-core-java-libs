package com.netcracker.cloud.dbaas.client.it.cassandra;

import com.netcracker.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasCassandra;
import com.netcracker.cloud.dbaas.client.it.cassandra.config.CassandraTestConfiguration;
import com.netcracker.cloud.dbaas.client.it.cassandra.config.CassandraTestContainer;
import com.netcracker.cloud.dbaas.client.it.cassandra.config.DataInitializePostConnectProcessor;
import com.netcracker.cloud.dbaas.client.it.cassandra.config.ServiceDataAccessConfiguration;
import com.netcracker.cloud.dbaas.client.it.cassandra.service.ServiceFilm;
import com.netcracker.cloud.dbaas.client.it.cassandra.service.ServiceFilmRepository;
import com.netcracker.cloud.dbaas.client.management.DatabasePool;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaasClassifierFactory;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.UUID;

import lombok.extern.slf4j.Slf4j;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;

@ExtendWith(SpringExtension.class)
@WebAppConfiguration
@Import({ DataInitializePostConnectProcessor.class })
@ContextConfiguration(classes = {
        CassandraTestConfiguration.class,
        ServiceDataAccessConfiguration.class })
@TestPropertySource(properties = {
        "cloud.microservice.namespace=test-namespace",
        "cloud.microservice.name=test-service",
})
@EnableDbaasCassandra
@Slf4j
public class CassandraProxyTest {

    @MockitoSpyBean
    private DatabasePool pool;

    @Autowired
    private ServiceFilmRepository serviceRepository;

    @Autowired
    private DbaasClassifierFactory classifierFactory;

    CassandraTestContainer container = CassandraTestContainer.getInstance();

    @Test
    void testRecreateSessionInCaseCassandraIsDown() throws InterruptedException {
        DbaaSChainClassifierBuilder classifierBuilder = classifierFactory.newServiceClassifierBuilder();
        classifierBuilder.withProperty("namespace", "test-namespace");
        classifierBuilder.withProperty("microserviceName", "test-service");

        // save calls method session.execute() two times in steps:
        // 1. PrepareStatement
        // 2. Execute prepared statement
        // see org.springframework.data.cassandra.core.cql.CqlTemplate.query
        serviceRepository.save(new ServiceFilm(UUID.randomUUID(), "title", 2014));
        container.stop();
        container.start();
        // save calls session.execute() two times plus one time for session recreate
        Assertions.assertDoesNotThrow(() -> serviceRepository.save(new ServiceFilm(UUID.randomUUID(), "title 2", 2015)));

        Mockito.verify(pool, times(1)).removeCachedDatabase(eq(CassandraDBType.INSTANCE),
                eq(classifierBuilder.build()));
        // getOrCreateDatabase is called for each session.execute()
        Mockito.verify(pool, times(5)).getOrCreateDatabase(eq(CassandraDBType.INSTANCE), eq(classifierBuilder.build()),
                any());
    }
}
