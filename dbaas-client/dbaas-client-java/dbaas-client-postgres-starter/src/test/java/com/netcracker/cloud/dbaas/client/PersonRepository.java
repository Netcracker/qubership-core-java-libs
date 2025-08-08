package com.netcracker.cloud.dbaas.client;


import org.springframework.data.repository.CrudRepository;

import java.util.List;

public interface PersonRepository extends CrudRepository<Person, Long> {

    List<Person> findByFirstName(String firstName);

    List<Person> findByLastName(String lastName);

}
