package com.netcracker.cloud.dbaas.client.test;


import org.springframework.data.annotation.Id;

public class Person {

    @Id
    private String id;
    private String firstName;
    private String lastName;

    public Person() {
    }

    public Person(String firstName, String lastName) {
        this.firstName = firstName;
        this.lastName = lastName;
    }

    @Override
    public String toString() {
        return String.format("Customer[id=%s, firstName='%s', lastName='%s']", id,
                firstName, lastName);
    }


}
