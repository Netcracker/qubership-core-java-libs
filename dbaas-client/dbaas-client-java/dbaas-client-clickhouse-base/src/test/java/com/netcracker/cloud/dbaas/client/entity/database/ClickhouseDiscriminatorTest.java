package com.netcracker.cloud.dbaas.client.entity.database;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ClickhouseDiscriminatorTest {

    @Test
    void testGetValueWithUserRole() {
        ClickhouseDiscriminator clickhouseDiscriminator = ClickhouseDiscriminator.builder()
                .userRole("role")
                .build();
        assertEquals("role", clickhouseDiscriminator.getValue());
    }

    @Test
    void testGetValueWithCustomDiscriminator() {
        ClickhouseDiscriminator clickhouseDiscriminator = ClickhouseDiscriminator.builder()
                .customDiscriminator("custom")
                .build();
        assertEquals("custom", clickhouseDiscriminator.getValue());
    }

    @Test
    void testGetValueWithNullUserRoleAndSchema() {
        ClickhouseDiscriminator clickhouseDiscriminator = ClickhouseDiscriminator.builder().build();
        assertNull(clickhouseDiscriminator.getValue());
    }
}
