package org.qubership.cloud.dbaas.client.it.mongodb;

import lombok.*;
import org.springframework.data.annotation.Id;

import java.util.UUID;

@Data
@AllArgsConstructor
public class SampleEntity {
    @Id
    private UUID id;
    private String text;
}

