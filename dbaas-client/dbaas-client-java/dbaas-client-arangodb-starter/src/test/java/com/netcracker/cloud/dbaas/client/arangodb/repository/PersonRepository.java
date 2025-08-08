package com.netcracker.cloud.dbaas.client.arangodb.repository;

import com.arangodb.springframework.repository.ArangoRepository;
import com.netcracker.cloud.dbaas.client.arangodb.entity.Person;

import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends ArangoRepository<Person, String> {
}
