package org.qubership.cloud.dbaas.client.it.postgresql.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

import static org.qubership.cloud.dbaas.client.config.DbaasPostgresConfiguration.SERVICE_POSTGRES_DATASOURCE;

@Configuration
@EnableJpaRepositories(value = SERVICE_POSTGRES_DATASOURCE,
        basePackages = "org.qubership.cloud.dbaas.client.it.postgresql.service",
        entityManagerFactoryRef = "serviceEntityManager",
        transactionManagerRef = "serviceTransactionManager")
public class ServiceDataAccessConfiguration {
    @Bean
    @Lazy
    @Qualifier("serviceTransactionManager")
    @Primary
    public JpaTransactionManager serviceTrans(@Autowired @Qualifier("serviceEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean.getObject());
        return transactionManager;
    }

    @Bean
    @Lazy
    @Qualifier("serviceEntityManager")
    public LocalContainerEntityManagerFactoryBean serviceEntityManager(@Autowired @Lazy
                                                                       @Qualifier(SERVICE_POSTGRES_DATASOURCE) DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setDataSource(dataSource);

        entityManagerFactory.setPackagesToScan("org.qubership.cloud.dbaas.client.it.postgresql.service");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

        Properties additionalProperties = new Properties();
        additionalProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        additionalProperties.put("hibernate.show_sql", "true");
        additionalProperties.put("hibernate.hbm2ddl.auto", "create");
        entityManagerFactory.setJpaProperties(additionalProperties);

        return entityManagerFactory;
    }
}

