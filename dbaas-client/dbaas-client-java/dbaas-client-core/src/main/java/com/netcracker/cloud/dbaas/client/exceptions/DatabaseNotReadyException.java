package com.netcracker.cloud.dbaas.client.exceptions;

import lombok.Getter;

@Getter
public class DatabaseNotReadyException extends RuntimeException {

    public DatabaseNotReadyException() {
        super();
    }
}
