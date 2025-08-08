package com.netcracker.cloud.dbaas.client.it.postgresql;

import org.qubership.cloud.dbaas.client.it.postgresql.service.Person;
import org.qubership.cloud.dbaas.client.it.postgresql.service.PersonServiceRepository;
import org.qubership.cloud.dbaas.client.it.postgresql.tenant.Customer;
import org.qubership.cloud.dbaas.client.it.postgresql.tenant.PersonTenantRepository;
import jakarta.persistence.EntityManager;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping(path = "/db/microservice")
@Slf4j
public class TestController {

    @Autowired
    private PersonServiceRepository serviceRepository;

    @Autowired
    private PersonTenantRepository tenantRepository;

    @Autowired
    @Qualifier("serviceEntityManager")
    EntityManager entityManager;

    @PostMapping(path = "/service")
    public ResponseEntity<String> saveIntoServiceDb(@RequestParam String fName, @RequestParam String lName) {
        Person savedEntity = serviceRepository.save(new Person(fName, lName));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity.getId().toString());
    }

    @PostMapping(path = "/tenant")
    public ResponseEntity<String> saveIntoTenantDb(@RequestParam String fName, @RequestParam String lName) {
        Customer savedEntity = tenantRepository.save(new Customer(fName, lName));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity.getId().toString());
    }

    @Transactional
    @PostMapping(path = "/serviceId")
    public ResponseEntity<String> getFromServiceById(@RequestParam String id) {
        String nameById= serviceRepository.findById(Long.valueOf(id)).get().getFirstName();
        return ResponseEntity.status(HttpStatus.CREATED).body(nameById);
    }

    @Transactional
    @PostMapping(path = "/service/transactional")
    public ResponseEntity<String> transactionalSleep(@RequestParam Integer milliseconds) throws InterruptedException {
        entityManager.persist(new Person("fName", "lName"));
        Thread.sleep(milliseconds);
        return ResponseEntity.status(HttpStatus.OK).body("");
    }

    @PostMapping(path = "/tenantId")
    public ResponseEntity<String> getFromTenantById(@RequestParam String id) {
        String nameById= tenantRepository.findById(Long.valueOf(id)).get().getFirstName();
        return ResponseEntity.status(HttpStatus.CREATED).body(nameById);
    }

    @GetMapping(path = "/service")
    public ResponseEntity<String> getAllFromServiceDb() {
        List<Person> result = new ArrayList<>();
        serviceRepository.findAll().forEach(result::add);
        return ResponseEntity.status(HttpStatus.OK).body(result.get(0).getFirstName());
    }

    @GetMapping(path = "/tenant")
    public ResponseEntity<String> getAllFromTenantDb() {
        List<Customer> result = new ArrayList<>();
        tenantRepository.findAll().forEach(result::add);
        return ResponseEntity.status(HttpStatus.OK).body(result.get(0).getFirstName());
    }

    @DeleteMapping(path = "/clear")
    public ResponseEntity<String> clearAllRepositories() {
        tenantRepository.deleteAll();
        serviceRepository.deleteAll();
        return ResponseEntity.status(HttpStatus.OK).body("Success");
    }
}
