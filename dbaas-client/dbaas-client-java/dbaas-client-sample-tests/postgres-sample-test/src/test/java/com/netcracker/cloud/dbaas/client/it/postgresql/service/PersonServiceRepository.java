package com.netcracker.cloud.dbaas.client.it.postgresql.service;


import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonServiceRepository extends CrudRepository<Person, Long> {
}
