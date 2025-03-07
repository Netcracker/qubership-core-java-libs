package org.qubership.cloud.dbaas.client.arangodb.entity;


import com.arangodb.springframework.annotation.ArangoId;
import com.arangodb.springframework.annotation.Document;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.qubership.cloud.dbaas.client.arangodb.test.configuration.PersonSerializer;
import lombok.Data;
import lombok.ToString;

import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
@ToString
@Document("persons")
@JsonSerialize(using = PersonSerializer.class)
public class Person {

    @Id
    private String id;
    @ArangoId
    private String arangoId;

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
