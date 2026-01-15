package com.netcracker.cloud.quarkus.dbaas.mongoclient.config.properties;

import com.netcracker.cloud.dbaas.client.entity.MongoDatabaseSettings;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.util.Optional;

@NoArgsConstructor
@AllArgsConstructor
public class MongoDbConfiguration {
    private Optional<DbaasMongoDbCreationConfig.InternalMongoDatabaseSettings> internalDatabaseSettings;

    public Optional<MongoDatabaseSettings> getDatabaseSettings() {
        return internalDatabaseSettings.map(DbaasMongoDbCreationConfig.InternalMongoDatabaseSettings::toMongoSettings);
    }
}
