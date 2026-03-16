package com.netcracker.cloud.dbaas.common.postprocessor;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import com.netcracker.cloud.dbaas.client.management.PostConnectProcessor;

import java.util.Comparator;

public interface QuarkusPostConnectProcessor<T extends AbstractDatabase> extends PostConnectProcessor<T> {
    Comparator<QuarkusPostConnectProcessor<?>> POST_PROCESSOR_ORDER = (o1, o2) -> Integer.compare(o1.getOrder(), o2.getOrder());

    default int getOrder() {
        return 0;
    }

    @Override
    default Class<T> getSupportedDatabaseType() {
        throw new IllegalStateException("This method should never be called in quarkus application");
    }
}
