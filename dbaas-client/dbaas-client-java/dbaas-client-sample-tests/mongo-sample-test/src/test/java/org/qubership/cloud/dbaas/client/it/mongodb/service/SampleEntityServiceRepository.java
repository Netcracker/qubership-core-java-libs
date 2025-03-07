package org.qubership.cloud.dbaas.client.it.mongodb.service;

import org.qubership.cloud.dbaas.client.it.mongodb.SampleEntity;
import org.springframework.stereotype.Repository;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.UUID;

@Repository
public interface SampleEntityServiceRepository extends MongoRepository<SampleEntity, UUID> {
}

