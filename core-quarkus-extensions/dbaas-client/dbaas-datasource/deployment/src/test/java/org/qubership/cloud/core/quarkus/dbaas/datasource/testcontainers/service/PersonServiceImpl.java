package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.service;

import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.Person;
import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.repository.PersonRepository;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import java.util.List;
import java.util.function.Function;

@ApplicationScoped
public class PersonServiceImpl implements PersonService {

    @Inject
    PersonRepository connectionRepository;

    @Transactional
    @Override
    public Person save(Person person) {
        connectionRepository.persist(person);
        return connectionRepository.findById(person.getId());
    }

    @Override
    public List<Person> findAll() {
        return connectionRepository.listAll();
    }

    @Transactional
    @Override
    public Person saveThenApply(Person person, Function<Person, Person> operation) {
        Person beforeOperation = save(person);
        return operation.apply(beforeOperation);
    }

    @Override
    public void deleteAll() {
        connectionRepository.deleteAll();
    }
}
