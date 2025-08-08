package com.netcracker.cloud.dbaas.client.it.cassandra;

import org.qubership.cloud.dbaas.client.it.cassandra.service.ServiceFilm;
import org.qubership.cloud.dbaas.client.it.cassandra.service.ServiceFilmRepository;
import org.qubership.cloud.dbaas.client.it.cassandra.tenant.TenantFilm;
import org.qubership.cloud.dbaas.client.it.cassandra.tenant.TenantFilmRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping(path = "/db/microservice")
public class TestController {

    @Autowired
    private ServiceFilmRepository serviceRepository;

    @Autowired
    private TenantFilmRepository tenantRepository;

    @PostMapping(path = "/service")
    public ResponseEntity<ServiceFilm> saveIntoServiceDb(@RequestParam UUID id, @RequestParam String title, @RequestParam int year) {
        ServiceFilm savedEntity = serviceRepository.save(new ServiceFilm(id, title, year));
        return ResponseEntity.status(HttpStatus.CREATED).body(savedEntity);
    }

    @PostMapping(path = "/tenant")
    public ResponseEntity<TenantFilm> saveIntoTenantDb(@RequestParam UUID id, @RequestParam String title, @RequestParam int year) {
        TenantFilm savedEntity = tenantRepository.save(new TenantFilm(id, title, year));
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

