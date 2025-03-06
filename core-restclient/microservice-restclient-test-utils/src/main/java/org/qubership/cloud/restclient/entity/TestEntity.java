package org.qubership.cloud.restclient.entity;

import java.util.UUID;

import lombok.Data;

@Data
public class TestEntity {
    private UUID id;
    private String name;

    public static TestEntity createTestEntity() {
        TestEntity testEntity = new TestEntity();
        testEntity.setId(UUID.randomUUID());
        testEntity.setName("TestEntity_" + testEntity.getId().toString());
        return testEntity;
    }
}
