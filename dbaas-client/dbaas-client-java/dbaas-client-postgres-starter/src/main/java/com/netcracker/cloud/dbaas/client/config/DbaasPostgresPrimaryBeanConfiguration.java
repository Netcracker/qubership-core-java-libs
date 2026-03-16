package com.netcracker.cloud.dbaas.client.config;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;

import javax.sql.DataSource;

import static com.netcracker.cloud.dbaas.client.DbaasConst.SERVICE;
import static com.netcracker.cloud.dbaas.client.DbaasConst.TENANT;
import static com.netcracker.cloud.dbaas.client.config.DbaasPostgresConfiguration.*;

@Configuration
public class DbaasPostgresPrimaryBeanConfiguration {

    private static final String DBAAS_API_POSTGRES_PRIMARY_BEAN = "dbaas.api.postgres.datasource.primary-bean";

    @Primary
    @Bean(name = {DATASOURCE})
    @ConditionalOnMissingBean(name = DATASOURCE)
    public DataSource dataSource(@Value("${" + DBAAS_API_POSTGRES_PRIMARY_BEAN + ":" + SERVICE + "}") String primaryBean,
                                 @Qualifier(SERVICE_POSTGRES_DATASOURCE) DataSource servicePostgresDatasource,
                                 @Qualifier(TENANT_POSTGRES_DATASOURCE) DataSource tenantPostgresDatasource) {
        if (SERVICE.equals(primaryBean)) {
            return servicePostgresDatasource;
        } else if (TENANT.equals(primaryBean)) {
            return tenantPostgresDatasource;
        }
        throw new RuntimeException(String.format("property %s=%s must have either %s or %s value",
                DBAAS_API_POSTGRES_PRIMARY_BEAN, primaryBean, SERVICE, TENANT));
    }
}
