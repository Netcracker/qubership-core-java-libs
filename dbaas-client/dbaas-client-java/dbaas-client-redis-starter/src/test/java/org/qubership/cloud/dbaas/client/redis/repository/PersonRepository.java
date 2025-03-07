package org.qubership.cloud.dbaas.client.redis.repository;


import org.qubership.cloud.dbaas.client.redis.entity.Person;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonRepository extends CrudRepository<Person, String> {
}
