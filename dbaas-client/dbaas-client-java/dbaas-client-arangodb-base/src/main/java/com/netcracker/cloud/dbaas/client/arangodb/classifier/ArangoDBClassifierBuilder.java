package com.netcracker.cloud.dbaas.client.arangodb.classifier;

import com.netcracker.cloud.dbaas.client.management.DbaasDbClassifier;
import com.netcracker.cloud.dbaas.client.management.classifier.DbaaSChainClassifierBuilder;

public class ArangoDBClassifierBuilder extends DbaaSChainClassifierBuilder {
    public static final String DB_ID_CLASSIFIER_PROPERTY = "dbId";

    private String dbId;

    public ArangoDBClassifierBuilder(DbaaSChainClassifierBuilder next) {
        super(next);
        dbId = "default";
    }

    public ArangoDBClassifierBuilder withDbId(String dbId) {
        this.dbId = dbId;
        return this;
    }

    @Override
    public DbaasDbClassifier build() {
        return new DbaasDbClassifier.Builder()
                .withProperties(super.build().asMap())
                .withProperty(DB_ID_CLASSIFIER_PROPERTY, dbId)
                .build();
    }
}
