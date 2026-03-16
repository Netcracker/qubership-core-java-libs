package com.netcracker.cloud.restlegacy.restclient.error;

/**
 * Exception can be thrown when system can't find BUSINESS entity.
 * If we can not find some INTERNAL entity we should NOT throw this exception.
 */
public class EntityNotFoundException extends RuntimeException {
    private final String entityTypeName;
    private final String entityId;

    public String getEntityTypeName() {
        return entityTypeName;
    }

    public String getEntityId() {
        return entityId;
    }

    public EntityNotFoundException(String entityTypeName, String entityId) {
        super(String.format("Entity %s not found for ID=%s", entityTypeName, entityId));
        this.entityTypeName = entityTypeName;
        this.entityId = entityId;
    }

}
