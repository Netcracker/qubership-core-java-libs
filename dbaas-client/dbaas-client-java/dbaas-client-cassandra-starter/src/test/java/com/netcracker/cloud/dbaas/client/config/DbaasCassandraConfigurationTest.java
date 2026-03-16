package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.cassandra.entity.DbaasCassandraProperties;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.cassandra.core.CassandraOperations;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import static com.netcracker.cloud.dbaas.client.config.DbaasCassandraConfiguration.*;

@EnableDbaasCassandra
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = TestConfig.class)
public class DbaasCassandraConfigurationTest {

    @Autowired
    @Qualifier(SERVICE_CASSANDRA_TEMPLATE)
    private CassandraOperations serviceCassandraTemplate;

    @Autowired
    @Qualifier(TENANT_CASSANDRA_TEMPLATE)
    private CassandraOperations tenantCassandraTemplate;

    @Autowired
    private DbaasCassandraProperties dbaasCassandraProperties;

    @Test
    public void checkServiceCassandraTemplateExisting() {
        Assertions.assertNotNull(serviceCassandraTemplate);
    }

    @Test
    public void checkTenantCassandraTemplateExisting() {
        Assertions.assertNotNull(tenantCassandraTemplate);
    }
}
