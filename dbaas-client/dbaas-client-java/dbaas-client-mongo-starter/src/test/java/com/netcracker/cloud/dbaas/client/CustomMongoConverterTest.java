package com.netcracker.cloud.dbaas.client;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.qubership.cloud.context.propagation.core.ContextManager;
import org.qubership.cloud.dbaas.client.test.configuration.MongoTestContainerConfiguration;
import org.qubership.cloud.dbaas.client.test.configuration.TestMongoRepositoriesConfiguration;
import org.qubership.cloud.framework.contexts.tenant.TenantContextObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.convert.DefaultDbRefResolver;
import org.springframework.data.mongodb.core.convert.MappingMongoConverter;
import org.springframework.data.mongodb.core.convert.MongoConverter;
import org.springframework.data.mongodb.core.mapping.DBRef;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.MongoMappingContext;
import org.springframework.test.context.TestContextManager;
import org.springframework.test.context.junit.jupiter.SpringExtension;

import java.util.UUID;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.qubership.cloud.dbaas.client.config.DbaasMongoConfiguration.*;
import static org.qubership.cloud.framework.contexts.tenant.BaseTenantProvider.TENANT_CONTEXT_NAME;

public class CustomMongoConverterTest {
    private static final UUID ENTITY_ID = UUID.fromString("6b81580f-9cae-458f-a3b5-08050129bc79");
    private static final UUID RELATED_ENTITY_ID = UUID.fromString("e1460cc4-315f-46a6-8bf1-5b289c8d2080");
    private static final String TENANT_ID = "test-tenant";

    @Autowired
    @Qualifier(SERVICE_MONGO_TEMPLATE)
    MongoTemplate serviceMongoTemplate;

    @Autowired
    @Qualifier(TENANT_MONGO_TEMPLATE)
    MongoTemplate tenantMongoTemplate;

    @BeforeEach
    void setUp() {
        ContextManager.set(TENANT_CONTEXT_NAME, new TenantContextObject(TENANT_ID));
    }

    private void initContext(Class<?> testClass) throws Exception {
        TestContextManager testContextManager = new TestContextManager(testClass);
        testContextManager.prepareTestInstance(this);
    }

    @Test
    public void testDefaultMongoConverter() throws Exception {
        initContext(DefaultConfig.class);
        assertFalse(serviceMongoTemplate.getConverter() instanceof CustomMappingMongoConverter);
        assertFalse(tenantMongoTemplate.getConverter() instanceof CustomMappingMongoConverter);
        assertNotEquals(serviceMongoTemplate.getConverter(), tenantMongoTemplate.getConverter());

        testTemplates();
    }

    @Test
    public void testCustomMongoConverter() throws Exception {
        initContext(CustomConfig.class);
        assertInstanceOf(CustomMappingMongoConverter.class, serviceMongoTemplate.getConverter());
        assertInstanceOf(CustomMappingMongoConverter.class, tenantMongoTemplate.getConverter());
        assertNotEquals(serviceMongoTemplate.getConverter(), tenantMongoTemplate.getConverter());

        testTemplates();
    }

    private void testTemplates() {
        clean();
        testTemplate(serviceMongoTemplate);
        assertThat(serviceMongoTemplate.findAll(Entity.class), not(empty()));
        assertThat(tenantMongoTemplate.findAll(Entity.class), empty());

        clean();
        testTemplate(tenantMongoTemplate);
        assertThat(serviceMongoTemplate.findAll(Entity.class), empty());
        assertThat(tenantMongoTemplate.findAll(Entity.class), not(empty()));
    }

    private void testTemplate(MongoTemplate mongoTemplate) {
        Entity relatedEntity = new Entity(RELATED_ENTITY_ID, "Related Entity", null);
        Entity entity = new Entity(ENTITY_ID, "Main Entity", relatedEntity);
        mongoTemplate.save(relatedEntity);
        mongoTemplate.save(entity);
        Entity loadedEntity = mongoTemplate.findById(entity.getId(), Entity.class);
        assertNotNull(loadedEntity);
        assertThat(loadedEntity.getRelatedEntity(), equalTo(relatedEntity));
    }

    private void clean() {
        if (serviceMongoTemplate.collectionExists(Entity.class)) {
            serviceMongoTemplate.dropCollection(Entity.class);
        }
        if (tenantMongoTemplate.collectionExists(Entity.class)) {
            tenantMongoTemplate.dropCollection(Entity.class);
        }
        assertThat(serviceMongoTemplate.findAll(Entity.class), empty());
        assertThat(tenantMongoTemplate.findAll(Entity.class), empty());
    }

    static class CustomMappingMongoConverter extends MappingMongoConverter {
        public CustomMappingMongoConverter(MongoDatabaseFactory factory) {
            super(new DefaultDbRefResolver(factory), new MongoMappingContext());
        }
    }

    @Configuration
    static class CustomMongoConverterConfiguration {
        @Bean(name = {SERVICE_MONGO_CONVERTER})
        public MongoConverter serviceMongoConverter(@Qualifier(SERVICE_MONGO_DB_FACTORY) MongoDatabaseFactory factory) {
            return new CustomMappingMongoConverter(factory);
        }

        @Bean(name = {TENANT_MONGO_CONVERTER})
        public MongoConverter tenantMongoConverter(@Qualifier(TENANT_MONGO_DB_FACTORY) MongoDatabaseFactory factory) {
            return new CustomMappingMongoConverter(factory);
        }
    }

    @ExtendWith(SpringExtension.class)
    @Import({MongoTestContainerConfiguration.class, TestMongoRepositoriesConfiguration.class})
    private static class DefaultConfig {
    }

    @ExtendWith(SpringExtension.class)
    @Import({MongoTestContainerConfiguration.class, TestMongoRepositoriesConfiguration.class, CustomMongoConverterConfiguration.class})
    private static class CustomConfig {
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Document("entities")
    static class Entity {
        @Id
        public UUID id;

        public String name;

        @DBRef
        public Entity relatedEntity;
    }
}
