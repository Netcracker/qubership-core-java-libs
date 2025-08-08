package org.qubership.cloud.dbaas.client.entity.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class PostgresqlDiscriminatorTest {

    @Test
    public void testGetValueWithUserRoleAndSchema() {
        PostgresqlDiscriminator postgresqlDiscriminator = PostgresqlDiscriminator.builder()
                .userRole("role")
                .schema("schema")
                .build();
        assertEquals("role:schema:roReplica=false", postgresqlDiscriminator.getValue());
    }

    @Test
    public void testGetValueWithNullUserRole() {
        PostgresqlDiscriminator postgresqlDiscriminator = PostgresqlDiscriminator.builder()
                .schema("schema")
                .build();
        assertEquals("schema:roReplica=false", postgresqlDiscriminator.getValue());
    }

    @Test
    public void testGetValueWithNullSchema() {
        PostgresqlDiscriminator postgresqlDiscriminator = PostgresqlDiscriminator.builder()
                .userRole("role")
                .build();
        assertEquals("role:roReplica=false", postgresqlDiscriminator.getValue());
    }

    @Test
    public void testGetValueWithCustomDiscriminator() {
        PostgresqlDiscriminator postgresqlDiscriminator = PostgresqlDiscriminator.builder()
                .customDiscriminator("custom")
                .build();
        assertEquals("custom", postgresqlDiscriminator.getValue());
    }

    @Test
    public void testGetValueWithNullUserRoleAndSchema() {
        PostgresqlDiscriminator postgresqlDiscriminator = PostgresqlDiscriminator.builder().build();
        assertNotNull(postgresqlDiscriminator.getValue());
    }

    @Test
    public void testGetValueWithRoReplicaAndSchema() {
        PostgresqlDiscriminator postgresqlDiscriminator = PostgresqlDiscriminator.builder()
                .roReplica(true)
                .schema("schema")
                .build();
        assertEquals("schema:roReplica=true", postgresqlDiscriminator.getValue());
    }
}