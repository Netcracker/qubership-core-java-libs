package com.netcracker.cloud.dbaas.client.opensearch.entity;

import com.netcracker.cloud.dbaas.client.entity.DbaasApiProperties;
import lombok.Data;

@Data
public class OpensearchProperties extends DbaasApiProperties {
    private OpensearchDbScopeProperties service = new OpensearchDbScopeProperties();
    private OpensearchDbScopeProperties tenant = new OpensearchDbScopeProperties();

    @Data
    public static class OpensearchDbScopeProperties extends DbaasApiProperties.DbScopeProperties {
        private String delimiter = "_";
        private String prefix;
    }
}
