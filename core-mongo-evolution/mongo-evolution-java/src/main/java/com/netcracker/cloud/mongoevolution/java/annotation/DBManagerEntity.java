package com.netcracker.cloud.mongoevolution.java.annotation;

import com.mongodb.client.MongoDatabase;
import lombok.Data;

@Data
public class DBManagerEntity {
    protected MongoDatabase mongoDatabase;
}
