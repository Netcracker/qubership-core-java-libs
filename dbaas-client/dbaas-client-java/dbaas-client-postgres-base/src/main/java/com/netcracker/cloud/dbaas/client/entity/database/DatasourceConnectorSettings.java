package com.netcracker.cloud.dbaas.client.entity.database;

import org.qubership.cloud.dbaas.client.service.flyway.FlywayRunner;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.HashMap;
import java.util.Map;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Builder
public class DatasourceConnectorSettings extends AbstractConnectorSettings {
    private String schema;
    private Discriminator discriminator;
    private Map<String, Object> connPropertiesParam = new HashMap<>();
    private FlywayRunner flywayRunner;
    private boolean roReplica;
}
