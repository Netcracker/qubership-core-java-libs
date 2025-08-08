package org.qubership.cloud.dbaas.client.it.mongodb;

import org.qubership.cloud.dbaas.client.it.mongodb.service.SampleEntityServiceRepository;
import org.qubership.cloud.dbaas.client.it.mongodb.tenant.SampleEntityTenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/db/microservice")
public class TestController {

    @Autowired
    private SampleEntityServiceRepository serviceRepository;

    @Autowired
    private SampleEntityTenantRepository tenantRepository;

    @PostMapping(path = "/insert")
    public ResponseEntity<List<SampleEntity>> save(@RequestParam UUID id, @RequestParam String name) {
        SampleEntity tenantFilm = new SampleEntity(id, name);
        List<SampleEntity> savedEntity = serviceRepository.insert(Collections.singletonList(tenantFilm));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
    }

    @PostMapping(path = "/service")
    public ResponseEntity<SampleEntity> saveIntoServiceDb(@RequestParam UUID id, @RequestParam String name) {
        SampleEntity savedEntity = serviceRepository.save(new SampleEntity(id, name));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
    }

    @PostMapping(path = "/tenant")
    public ResponseEntity<SampleEntity> saveIntoTenantDb(@RequestParam UUID id, @RequestParam String name) {
        SampleEntity savedEntity = tenantRepository.save(new SampleEntity(id, name));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
    }

    @GetMapping(path = "/service")
    public ResponseEntity<String> getAllFromServiceDb() {
        return ResponseEntity.status(HttpStatus.OK).body(serviceRepository.findAll().get(0).getId().toString());
    }

    @GetMapping(path = "/tenant")
    public ResponseEntity<String> getAllFromTenantDb() {
        return ResponseEntity.status(HttpStatus.OK).body(tenantRepository.findAll().get(0).getId().toString());
    }

    @DeleteMapping(path = "/clear")
    public ResponseEntity<String> clearAllRepositories() {
        tenantRepository.deleteAll();
        serviceRepository.deleteAll();
        return ResponseEntity.status(HttpStatus.OK).body("Success");
    }
}

