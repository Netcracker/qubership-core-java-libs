package com.netcracker.cloud.dbaas.client.redis.jpa;

import org.qubership.cloud.dbaas.client.redis.configuration.annotation.EnableDbaasRedisRepositories;
import org.qubership.cloud.dbaas.client.redis.configuration.annotation.EnableServiceDbaasRedis;
import org.qubership.cloud.dbaas.client.redis.entity.Person;
import org.qubership.cloud.dbaas.client.redis.repository.PersonRepository;
import org.qubership.cloud.dbaas.client.redis.test.configuration.TestRedisConfiguration;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {TestRedisConfiguration.class})
@WebAppConfiguration
@EnableRedisRepositories(basePackages = "org.qubership.cloud.dbaas.client.redis.repository")
@EnableDbaasRedisRepositories
@EnableServiceDbaasRedis
class RedisRepositoriesTest {

    private static final String FIRST_NAME = "John";
    private static final String LAST_NAME = "Doe";

    @Autowired
    private PersonRepository personRepository;

    @Test
    void testCrud() {
        Person person = new Person(FIRST_NAME, LAST_NAME);
        personRepository.save(person);
        Optional<Person> optionalFoundPerson = personRepository.findById(person.getId());
        assertTrue(optionalFoundPerson.isPresent());
        Person foundPerson = optionalFoundPerson.get();
        assertEquals(FIRST_NAME, foundPerson.getFirstName());
        assertEquals(LAST_NAME, foundPerson.getLastName());
        personRepository.deleteById(person.getId());
        optionalFoundPerson = personRepository.findById(person.getId());
        assertFalse(optionalFoundPerson.isPresent());
    }

}
