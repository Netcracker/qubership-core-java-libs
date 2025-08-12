package com.netcracker.cloud.dbaas.client.it.postgresql.tenant;

import org.springframework.data.repository.CrudRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface PersonTenantRepository extends CrudRepository<Customer, Long> {
}
