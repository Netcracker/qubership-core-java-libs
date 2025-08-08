package com.netcracker.cloud.dbaas.client.config;

import com.netcracker.cloud.dbaas.client.DbaasClient;
import com.netcracker.cloud.dbaas.client.management.*;
import com.netcracker.cloud.dbaas.client.service.LogicalDbProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.annotation.AnnotationAwareOrderComparator;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Configuration
public class DbaasPoolConfig {

    @Bean
    public DatabasePool dbaasConnectionPool(@Autowired DbaasClient dbaasClient,
                                            @Autowired(required = false) List<PostConnectProcessor<?>> postProcessors,
                                            @Autowired MSInfoProvider msInfoProvider,
                                            @Autowired(required = false) List<LogicalDbProvider<?, ?>> dbProviders,
                                            @Autowired(required = false) List<DatabaseClientCreator<?, ?>> dbClientCreators,
                                            @Autowired DatabaseDefinitionHandler databaseDefinitionHandler) {
        // validate postProcessors order, check that only default post processors has negative (highest priority order)
        PostProcessorComparator postProcessorComparator = new PostProcessorComparator();

        List<String> postProcessorsWithIllegalOrder = postProcessors != null ? postProcessors.stream()
                .filter(postConnectProcessor -> postProcessorComparator.getPostprocessorOrder(postConnectProcessor) < 0)
                .filter(postConnectProcessor -> !postConnectProcessor.getClass().getPackage().getName().startsWith("com.netcracker.cloud.dbaas.client"))
                .map(postConnectProcessor -> postConnectProcessor.getClass().getName())
                .collect(Collectors.toList()) : Collections.emptyList();

        if (!postProcessorsWithIllegalOrder.isEmpty()) {
            throw new IllegalStateException(String.format("Postprocessors %s have negative Order value " +
                            "but are not default postprocessors. Change postprocessors' order value to the positive Integer.",
                    postProcessorsWithIllegalOrder.toString()));
        }

        return new DatabasePool(dbaasClient,
                msInfoProvider.getMicroserviceName(),
                msInfoProvider.getNamespace(),
                postProcessors,
                databaseDefinitionHandler,
                AnnotationAwareOrderComparator.INSTANCE,
                dbProviders,
                dbClientCreators);
    }

    @Bean
    public DatabaseDefinitionHandler databaseDefinitionHandler(Optional<List<DatabaseDefinitionProcessor<?>>> databaseDefinitionProcessors,
                                                               Optional<List<DatabaseClientCreator<?,?>>> databaseClientCreators,
                                                               DbaasClient dbaasClient) {
        return new DatabaseDefinitionHandler(databaseDefinitionProcessors, databaseClientCreators, dbaasClient);
    }


    public class PostProcessorComparator extends AnnotationAwareOrderComparator {
        public int getPostprocessorOrder(PostConnectProcessor<?> postConnectProcessor) {
            return this.getOrder(postConnectProcessor);
        }
    }
}
