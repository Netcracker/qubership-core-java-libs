package com.netcracker.cloud.dbaas.client.redis.entity;


import lombok.Data;
import lombok.ToString;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;

import java.util.UUID;

@Data
@ToString
@RedisHash("Person")
public class Person {

    @Id
    private String id;
    private String firstName;
    private String lastName;

    public Person() {
        id = UUID.randomUUID().toString();
    }

    public Person(String firstName, String lastName) {
        id = UUID.randomUUID().toString();
        this.firstName = firstName;
        this.lastName = lastName;
    }
}
