package com.netcracker.cloud.bluegreen.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.logging.Logger;
import com.netcracker.cloud.bluegreen.quarkus.config.*;

public class BlueGreenStateMonitorProcessor {

    private static final String FEATURE = "blue-green-state-monitor";

    private static final Logger log = Logger.getLogger(BlueGreenStateMonitorProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem registerAdditionalBeans(InMemoryBlueGreenBuildTimeConfig inMemoryBlueGreenBuildTimeConfig,
                                                           BlueGreenBuildTimeConfig blueGreenBuildTimeConfig) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();

        boolean globalEnabled = inMemoryBlueGreenBuildTimeConfig.global().enabled();
        boolean devEnabled = inMemoryBlueGreenBuildTimeConfig.dev().enabled();

        boolean globalMutexEnabled = blueGreenBuildTimeConfig.globalMutex().enabled();
        boolean microserviceMutexEnabled = blueGreenBuildTimeConfig.microserviceMutex().enabled();
        boolean statePublisherEnabled = blueGreenBuildTimeConfig.statePublisher().enabled();

        if (!globalEnabled || devEnabled) {
            builder.addBeanClass(InMemoryConfiguration.class);
            builder.addBeanClass(BGStateSubscriberConfiguration.class);
            log.info("Enabled InMemoryConfiguration");
        } else {
            if (statePublisherEnabled) {
                builder.addBeanClass(ConsulBlueGreenStatePublisherConfiguration.class);
                builder.addBeanClass(BGStateSubscriberConfiguration.class);
                log.info("Enabled ConsulBlueGreenStatePublisherConfiguration");
            }
            if (globalMutexEnabled) {
                builder.addBeanClass(ConsulBlueGreenGlobalMutexConfiguration.class);
                log.info("Enabled ConsulBlueGreenGlobalMutexConfiguration");
            }
            if (microserviceMutexEnabled) {
                builder.addBeanClass(ConsulBlueGreenMicroserviceMutexConfiguration.class);
                log.info("Enabled ConsulBlueGreenMicroserviceMutexConfiguration");
            }
        }
        return builder.setUnremovable().build();
    }
}
