package org.qubership.cloud.dbaas.client.config.msframeworkspecific.testconfig;

import com.mongodb.client.MongoClients;
import org.qubership.cloud.dbaas.client.DbaasClient;
import org.qubership.cloud.dbaas.client.DbaasClientImpl;
import org.qubership.cloud.dbaas.client.config.DefaultMSInfoProvider;
import org.qubership.cloud.dbaas.client.config.EnableDbaasMongo;
import org.qubership.cloud.dbaas.client.config.MSInfoProvider;
import org.qubership.cloud.dbaas.client.management.DatabasePool;
import org.qubership.cloud.restclient.MicroserviceRestClient;
import de.bwaldvogel.mongo.MongoServer;
import de.bwaldvogel.mongo.backend.memory.MemoryBackend;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;

@Configuration
@EnableMongoRepositories(basePackages = "org.qubership.cloud.dbaas.client.config.msframeworkspecific")
@EnableDbaasMongo
public class TestMongoDbConfiguration {

    public final static String TARGET_DB_NAME = "testDb";
    public final static String DB_NAME = "dbName";

    public final static String DB_USER = "dbaas";
    public final static String AUTH_DB_NAME = "admin";
    public final static String DB_PASSWORD = "dbaas";
    public final static String MONGO_TEST_URI = "mongodb://"
            + DB_USER + ":" + DB_PASSWORD + "@localhost:27017,localhost:27018/";
    public static final String TENANT_ID = "test-tenant";

    @Primary
    @Bean
    @Qualifier("dbaasRestClient")
    public static MicroserviceRestClient testRestClient() {
        return Mockito.mock(MicroserviceRestClient.class);
    }

    @Bean(destroyMethod = "shutdown")
    public MongoServer mongoServer() {
        MongoServer mongoServer = new MongoServer(new MemoryBackend());
        mongoServer.bind();
        return mongoServer;
    }

    @Bean(destroyMethod = "close")
    public com.mongodb.client.MongoClient mongoClient() {
        return MongoClients.create(MONGO_TEST_URI);
    }

    @Primary
    @Bean
    public DbaasClient testDbaasClientMock() {
        return Mockito.mock(DbaasClientImpl.class);
    }

    @Primary
    @Bean
    public DatabasePool dbaasConnectionPoolMock() {
        return Mockito.mock(DatabasePool.class);
    }

    @Bean
    public MSInfoProvider msInfoProvider() {
        return Mockito.spy(new DefaultMSInfoProvider());
    }
}