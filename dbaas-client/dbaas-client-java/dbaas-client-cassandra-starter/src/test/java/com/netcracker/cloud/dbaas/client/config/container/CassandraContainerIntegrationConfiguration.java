package com.netcracker.cloud.dbaas.client.config.container;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.cassandra.entity.connection.CassandraDBConnection;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.CassandraDatabase;
import com.netcracker.cloud.dbaas.client.cassandra.entity.database.type.CassandraDBType;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasCassandra;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.restclient.MicroserviceRestClient;

import org.mockito.Mockito;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import java.net.InetSocketAddress;
import java.util.HashMap;
import java.util.List;
import java.util.TreeMap;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SCOPE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.TENANT;
import static com.netcracker.cloud.dbaas.client.DbaasConst.TENANT_ID;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasCassandra
public class CassandraContainerIntegrationConfiguration {
    public static final String TEST_ROLE = "test-role";
    public static final String SERVICE_KEYSPACE = "service_db";
    public static final String TENANT_KEYSPACE_A = "tenant_db_a";
    public static final String TENANT_KEYSPACE_B = "tenant_db_b";

    @Autowired
    @Qualifier("cassandraContainer")
    private CassandraTestContainer container;

    @Bean
    @Primary
    @Qualifier("dbaasRestClient")
    public MicroserviceRestClient mockDbaasRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Bean
    @Primary
    public DbaasClient getDbaasClient() {
        DbaasClient dbaasClient = Mockito.mock(DbaasClient.class);

        when(dbaasClient.getOrCreateDatabase(any(CassandraDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenAnswer((Answer<CassandraDatabase>) invocationOnMock -> {
                    HashMap<String, String> classifierFromMock = (HashMap<String, String>) invocationOnMock.getArguments()[2];
                    return getCassandraDatabase(classifierFromMock);
                });

        return dbaasClient;
    }

    private CassandraDatabase getCassandraDatabase(HashMap<String, String> classifier) {
        CassandraDBConnection cassandraDBConnection = new CassandraDBConnection();
        InetSocketAddress contactPoint = container.getContactPoint();
        cassandraDBConnection.setContactPoints(List.of(contactPoint.getHostString()));
        cassandraDBConnection.setPort(contactPoint.getPort());
        cassandraDBConnection.setUsername(container.getUsername());
        cassandraDBConnection.setPassword(container.getPassword());

        cassandraDBConnection.setRole(TEST_ROLE);

        String keyspace;
        if (TENANT.equals(classifier.get(SCOPE))) {
            keyspace = classifier.get(TENANT_ID);
        } else {
            keyspace = SERVICE_KEYSPACE;
        }
        cassandraDBConnection.setKeyspace(keyspace);
        CassandraDatabase cassandraDatabase = new CassandraDatabase();
        cassandraDatabase.setName(keyspace);
        cassandraDatabase.setConnectionProperties(cassandraDBConnection);
        cassandraDatabase.setClassifier(new TreeMap<>(classifier));
        return cassandraDatabase;
    }

}
