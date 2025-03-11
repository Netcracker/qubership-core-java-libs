package org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.repository;

import org.qubership.cloud.core.quarkus.dbaas.datasource.testcontainers.entity.Person;
import io.quarkus.hibernate.orm.panache.PanacheRepositoryBase;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.transaction.Transactional;

@Transactional
@ApplicationScoped
public class PersonRepository implements PanacheRepositoryBase<Person, Long> {

}
