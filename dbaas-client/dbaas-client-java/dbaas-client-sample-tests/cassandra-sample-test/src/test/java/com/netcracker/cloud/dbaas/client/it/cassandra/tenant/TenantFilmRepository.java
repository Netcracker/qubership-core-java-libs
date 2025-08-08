package com.netcracker.cloud.dbaas.client.it.cassandra.tenant;

import org.springframework.data.cassandra.repository.CassandraRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface TenantFilmRepository extends CassandraRepository<TenantFilm, UUID> {
}
