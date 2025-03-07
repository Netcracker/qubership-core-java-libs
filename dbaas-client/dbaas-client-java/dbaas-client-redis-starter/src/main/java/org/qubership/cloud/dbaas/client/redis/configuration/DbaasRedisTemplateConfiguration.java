package org.qubership.cloud.dbaas.client.redis.configuration;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.redis.core.RedisTemplate;

import static org.qubership.cloud.dbaas.client.DbaasConst.SERVICE;
import static org.qubership.cloud.dbaas.client.DbaasConst.TENANT;
import static org.qubership.cloud.dbaas.client.redis.configuration.ServiceDbaasRedisConfiguration.SERVICE_DBAAS_REDIS_TEMPLATE;
import static org.qubership.cloud.dbaas.client.redis.configuration.TenantDbaasRedisConfiguration.TENANT_DBAAS_REDIS_TEMPLATE;

@Configuration
public class DbaasRedisTemplateConfiguration {
    public static final String DBAAS_API_REDIS_PRIMARY_BEAN = "dbaas.redis.primary-bean";
    public static final String DEFAULT_REDIS_TEMPLATE_BEAN_NAME = "redisTemplate";

    @Primary
    @Bean(DEFAULT_REDIS_TEMPLATE_BEAN_NAME)
    @ConditionalOnMissingBean(name = DEFAULT_REDIS_TEMPLATE_BEAN_NAME)
    public RedisTemplate<String, Object> redisTemplate(@Value("${" + DBAAS_API_REDIS_PRIMARY_BEAN + ":" + SERVICE + "}") String primaryBean,
                                                       @Autowired(required = false) @Qualifier(SERVICE_DBAAS_REDIS_TEMPLATE) RedisTemplate<String, Object> serviceRedisTemplate,
                                                       @Autowired(required = false) @Qualifier(TENANT_DBAAS_REDIS_TEMPLATE) RedisTemplate<String, Object> tenantRedisTemplate) {
        if (serviceRedisTemplate == null) {
            return tenantRedisTemplate;
        }
        if (tenantRedisTemplate == null) {
            return serviceRedisTemplate;
        }
        if (SERVICE.equals(primaryBean)) {
            return serviceRedisTemplate;
        } else if (TENANT.equals(primaryBean)) {
            return tenantRedisTemplate;
        }
        throw new RuntimeException(String.format("property %s=%s must have either %s or %s value",
                DBAAS_API_REDIS_PRIMARY_BEAN, primaryBean, SERVICE, TENANT));
    }
}
