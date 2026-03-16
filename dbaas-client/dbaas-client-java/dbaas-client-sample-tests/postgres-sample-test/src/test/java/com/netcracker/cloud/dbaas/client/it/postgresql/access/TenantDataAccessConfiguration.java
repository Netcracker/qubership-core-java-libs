package com.netcracker.cloud.dbaas.client.it.postgresql.access;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import javax.sql.DataSource;
import java.util.Properties;

import static com.netcracker.cloud.dbaas.client.config.DbaasPostgresConfiguration.TENANT_POSTGRES_DATASOURCE;

@Configuration
@EnableJpaRepositories(value = TENANT_POSTGRES_DATASOURCE,
        basePackages = "com.netcracker.cloud.dbaas.client.it.postgresql.tenant",
        entityManagerFactoryRef = "tenantEntityManager",
        transactionManagerRef = "tenantTransactionManager")
public class TenantDataAccessConfiguration {
    @Bean
    @Lazy
    @Qualifier("tenantTransactionManager")
    public JpaTransactionManager tenantTrans(@Autowired @Qualifier("tenantEntityManager") LocalContainerEntityManagerFactoryBean entityManagerFactoryBean) {
        JpaTransactionManager transactionManager = new JpaTransactionManager();
        transactionManager.setEntityManagerFactory(entityManagerFactoryBean.getObject());
        return transactionManager;
    }

    @Bean
    @Lazy
    @Qualifier("tenantEntityManager")
    public LocalContainerEntityManagerFactoryBean tenantEntityManager(@Autowired @Qualifier(TENANT_POSTGRES_DATASOURCE) DataSource dataSource) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory =
                new LocalContainerEntityManagerFactoryBean();

        entityManagerFactory.setDataSource(dataSource);

        entityManagerFactory.setPackagesToScan("com.netcracker.cloud.dbaas.client.it.postgresql.tenant");

        HibernateJpaVendorAdapter vendorAdapter = new HibernateJpaVendorAdapter();
        entityManagerFactory.setJpaVendorAdapter(vendorAdapter);

        Properties additionalProperties = new Properties();
        additionalProperties.put("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        additionalProperties.put("hibernate.show_sql", "true");
        entityManagerFactory.setJpaProperties(additionalProperties);

        return entityManagerFactory;
    }
}
