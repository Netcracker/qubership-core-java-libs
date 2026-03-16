package com.netcracker.cloud.dbaas.client.arangodb.configuration;

import com.arangodb.serde.ArangoSerde;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DbaasArangoDBClientProperties {

    DbaasArangoDBConfigurationProperties properties;

    ArangoSerde arangoSerde;
}
