package org.qubership.cloud.mongoevolution.java;

import lombok.Data;
import org.bson.types.ObjectId;

@Data
public class TestObject {

    private ObjectId id;

    private String name;
}
