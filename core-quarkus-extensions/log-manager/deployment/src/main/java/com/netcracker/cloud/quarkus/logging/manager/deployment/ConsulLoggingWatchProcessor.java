package com.netcracker.cloud.quarkus.logging.manager.deployment;

import com.netcracker.cloud.quarkus.logging.manager.runtime.consul.ConsulLoggingConfigRecorder;
import com.netcracker.cloud.quarkus.logging.manager.runtime.consul.ConsulLoggingConfigWatchFactory;
import io.quarkus.deployment.annotations.BuildProducer;
import io.quarkus.deployment.annotations.BuildStep;
import io.quarkus.deployment.annotations.ExecutionTime;
import io.quarkus.deployment.annotations.Record;
import io.quarkus.deployment.builditem.FeatureBuildItem;
import io.quarkus.runtime.RuntimeValue;

class ConsulLoggingWatchProcessor {
    private static final String FEATURE = "consul-logging-watcher";

    @BuildStep
    public void feature(BuildProducer<FeatureBuildItem> feature) {
        feature.produce(new FeatureBuildItem(FEATURE));
    }

    @BuildStep
    @Record(ExecutionTime.RUNTIME_INIT)
    void configureLoggingWatch(ConsulLoggingConfigRecorder recorder) {
        RuntimeValue<ConsulLoggingConfigWatchFactory> factory = recorder.createConsulLoggingWatchFactory();
    }
}
