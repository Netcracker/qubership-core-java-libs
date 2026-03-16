package com.netcracker.cloud.dbaas.common.postprocessor;

import com.netcracker.cloud.dbaas.client.entity.database.AbstractDatabase;
import jakarta.enterprise.inject.Instance;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import static com.netcracker.cloud.dbaas.common.postprocessor.QuarkusPostConnectProcessor.POST_PROCESSOR_ORDER;

@Slf4j
@AllArgsConstructor
public class PostConnectProcessorManager<T extends AbstractDatabase> {

    Instance<QuarkusPostConnectProcessor<T>> postProcessors;

    public void applyPostProcessors(T database) {
        log.info("Applying post processors for database: {}", database);
        postProcessors.stream()
                .sorted(POST_PROCESSOR_ORDER)
                .forEach(postProcessor -> postProcessor.process(database));
        log.info("Finished applying post processors for database: {}", database);
    }
}
