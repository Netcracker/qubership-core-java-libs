package org.qubership.cloud.bluegreen.quarkus.deployment;

import io.quarkus.arc.deployment.AdditionalBeanBuildItem;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import org.jboss.logging.Logger;
import org.qubership.cloud.bluegreen.quarkus.config.*;

public class BlueGreenStateMonitorProcessor {

    private static final String FEATURE = "blue-green-state-monitor";

    private static final Logger log = Logger.getLogger(BlueGreenStateMonitorProcessor.class);

    @BuildStep
    FeatureBuildItem feature() {
        return new FeatureBuildItem(FEATURE);
    }

    @BuildStep
    public AdditionalBeanBuildItem registerAdditionalBeans(InMemoryBlueGreenBuildTimeConfig.BlueGreenGlobal blueGreenGlobal,
                                                           InMemoryBlueGreenBuildTimeConfig.Dev devConfig,
                                                           BlueGreenBuildTimeConfig.GlobalMutex globalMutexConfig,
                                                           BlueGreenBuildTimeConfig.MicroserviceMutex microserviceMutexConfig,
                                                           BlueGreenBuildTimeConfig.StatePublisher statePublisherConfig) {
        AdditionalBeanBuildItem.Builder builder = AdditionalBeanBuildItem.builder();
        if (!blueGreenGlobal.enabled || devConfig.enabled) {
            builder.addBeanClass(InMemoryConfiguration.class);
            log.info("Enabled InMemoryConfiguration");
        } else {
            if (statePublisherConfig.enabled) {
                builder.addBeanClass(ConsulBlueGreenStatePublisherConfiguration.class);
                log.info("Enabled ConsulBlueGreenStatePublisherConfiguration");
            }
            if (globalMutexConfig.enabled) {
                builder.addBeanClass(ConsulBlueGreenGlobalMutexConfiguration.class);
                log.info("Enabled ConsulBlueGreenGlobalMutexConfiguration");
            }
            if (microserviceMutexConfig.enabled) {
                builder.addBeanClass(ConsulBlueGreenMicroserviceMutexConfiguration.class);
                log.info("Enabled ConsulBlueGreenMicroserviceMutexConfiguration");
            }
        }
        return builder.setUnremovable().build();
    }

}
