package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.service;

import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.Person;

import java.util.List;
import java.util.function.Function;

public interface PersonService {
    Person save(Person person);
    List<Person> findAll();
    Person saveThenApply(Person person, Function<Person, Person> operation);
    void deleteAll();
}
