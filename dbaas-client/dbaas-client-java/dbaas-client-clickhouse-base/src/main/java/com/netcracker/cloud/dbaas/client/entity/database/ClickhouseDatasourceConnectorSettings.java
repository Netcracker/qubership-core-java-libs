package com.netcracker.cloud.dbaas.client.entity.database;

import lombok.*;
import lombok.extern.slf4j.Slf4j;

import java.util.Properties;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Builder
public class ClickhouseDatasourceConnectorSettings extends AbstractConnectorSettings {
    private Discriminator discriminator;
    private Properties datasourceProperties;
}
