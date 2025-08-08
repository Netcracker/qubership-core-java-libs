package com.netcracker.core.scheduler.po.repository;

public class VersionMismatchException extends RuntimeException{
    public VersionMismatchException(String message) {
        super(message);
    }
}
