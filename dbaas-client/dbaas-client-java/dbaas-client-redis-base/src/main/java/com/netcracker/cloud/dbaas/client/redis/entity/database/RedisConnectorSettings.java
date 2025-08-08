package com.netcracker.cloud.dbaas.client.redis.entity.database;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import com.netcracker.cloud.dbaas.client.entity.database.Discriminator;
import lombok.*;
import lombok.extern.slf4j.Slf4j;

@Data
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
@Slf4j
@Builder
public class RedisConnectorSettings extends AbstractConnectorSettings {
    private Discriminator discriminator;

    @Override
    public Discriminator getDiscriminator() {
        return discriminator;
    }
}
