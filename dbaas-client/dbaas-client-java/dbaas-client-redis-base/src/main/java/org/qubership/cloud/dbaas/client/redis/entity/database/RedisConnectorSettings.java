package org.qubership.cloud.dbaas.client.redis.entity.database;

import org.qubership.cloud.dbaas.client.entity.database.AbstractConnectorSettings;
import org.qubership.cloud.dbaas.client.entity.database.Discriminator;
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
