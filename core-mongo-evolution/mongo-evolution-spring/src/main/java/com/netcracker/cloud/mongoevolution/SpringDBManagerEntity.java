package com.netcracker.cloud.mongoevolution;

import com.netcracker.cloud.mongoevolution.java.annotation.DBManagerEntity;
import lombok.Data;
import org.springframework.data.mongodb.core.MongoTemplate;

@Data
public class SpringDBManagerEntity extends DBManagerEntity {
    private MongoTemplate mongoTemplate;
}
