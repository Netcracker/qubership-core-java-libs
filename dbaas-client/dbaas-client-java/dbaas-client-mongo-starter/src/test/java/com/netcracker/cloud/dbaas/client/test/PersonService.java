package org.qubership.cloud.dbaas.client.test;


import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

public class PersonService {

    @Autowired
    private PersonRepository personRepository;

    public void addPerson(Person person) {
        personRepository.insert(person);
    }

    public void deletePerson(Long id) {
        personRepository.deleteById(id);
    }

    public List<Person> findAll() {
        return personRepository.findAll();
    }

    public List<Person> findByFirstName(String firstName) {
        return personRepository.findByFirstName(firstName);
    }

    public List<Person> findByLastName(String lastName) {
        return personRepository.findByLastName(lastName);
    }
}
