package org.qubership.cloud.context.propagation.messaging.kafka.quarkus.deployment;

import org.qubership.cloud.context.propagaton.messaging.kafka.quarkus.ContextAwareEmitterProducer;
import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;

/**
 * Build structure definition
 */
public class MessagingContextPropagationDeployment {

    private static final String FEATURE = "messaging-context-propagation-deployment";

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    void build(BuildProducer<AdditionalBeanBuildItem> additionalBean) {
        additionalBean.produce(new AdditionalBeanBuildItem(ContextAwareEmitterProducer.class));
    }
}
