package com.netcracker.cloud.dbaas.client.it.mongodb.tenant;

import org.qubership.cloud.dbaas.client.it.mongodb.SampleEntity;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface SampleEntityTenantRepository extends MongoRepository<SampleEntity, UUID> {
}

