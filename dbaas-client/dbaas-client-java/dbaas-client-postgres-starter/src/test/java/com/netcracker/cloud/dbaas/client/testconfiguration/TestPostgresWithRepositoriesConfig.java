package com.netcracker.cloud.dbaas.client.testconfiguration;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.PersonService;
import com.netcracker.cloud.dbaas.client.config.EnableDbaasPostgresql;
import com.netcracker.cloud.dbaas.client.entity.connection.PostgresDBConnection;
import com.netcracker.cloud.dbaas.client.entity.database.PostgresDatabase;
import com.netcracker.cloud.dbaas.client.entity.database.type.PostgresDBType;
import com.netcracker.cloud.dbaas.client.management.DatabaseConfig;
import com.netcracker.cloud.restclient.MicroserviceRestClient;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.dao.annotation.PersistenceExceptionTranslationPostProcessor;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

import static com.netcracker.cloud.dbaas.client.DbaasConst.ADMIN_ROLE;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@Configuration
@EnableDbaasPostgresql
@EnableJpaRepositories(basePackages = "com.netcracker.cloud.dbaas.client",
        entityManagerFactoryRef = "localContainerEntityManagerFactoryBean")
public class TestPostgresWithRepositoriesConfig {
    int UNNECESSARY_URL_PARAMS_AMOUNT = 20;

    @Autowired
    @Qualifier("pgContainer")
    private PostgresqlContainerConfiguration container;

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

        when(dbaasClient.getOrCreateDatabase(any(PostgresDBType.class), anyString(), anyMap(), any(DatabaseConfig.class)))
                .thenReturn(getPostgresDb());

        return dbaasClient;
    }

    public PostgresDatabase getPostgresDb() {
        PostgresDatabase database = new PostgresDatabase();
        database.setName("test_db");

        String address = container.getJdbcUrl().substring(0, container.getJdbcUrl().length() - UNNECESSARY_URL_PARAMS_AMOUNT) + "test_db";
        PostgresDBConnection connection = new PostgresDBConnection(address, container.getUsername(), container.getPassword(), ADMIN_ROLE);
        database.setConnectionProperties(connection);

        return database;
    }

    @Bean
    public PersonService testPersonService() {
        return new PersonService();
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean localContainerEntityManagerFactoryBean(@Autowired DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setDataSource(dataSource);

        // Classpath scanning of @Component, @Service, etc annotated class
        entityManagerFactory.setPackagesToScan("com.netcracker.cloud.dbaas.client");

        // Vendor adapter
        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

        // Hibernate properties
        Properties additionalProperties = new Properties();
        additionalProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        additionalProperties.put("hibernate.show_sql", "true");
        additionalProperties.put("hibernate.hbm2ddl.auto", "update");
        entityManagerFactory.setJpaProperties(additionalProperties);

        return entityManagerFactory;
    }

    @Bean
    public JpaTransactionManager transactionManager(@Autowired LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean.getObject());
        return transactionManager;
    }

    @Bean
    public PersistenceExceptionTranslationPostProcessor exceptionTranslation() {
        return new PersistenceExceptionTranslationPostProcessor();
    }
}
