package com.netcracker.cloud.maas.bluegreen.kafka.util;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.testcontainers.containers.Container.ExecResult;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.utility.DockerImageName;

import java.io.IOException;

/**
 * Testcontainers 1.21.4 executes {@code /tmp/testcontainers_start.sh} as soon as the file exists.
 * On a busy runner the copy is still in progress, the script is busy, and the container exits 126.
 * The sentinel is created only after the copy returns.
 */
public class KafkaContainerStartedAfterScript extends KafkaContainer {
    private static final String SCRIPT = "/tmp/testcontainers_start.sh";
    private static final String READY = SCRIPT + ".ready";

    public KafkaContainerStartedAfterScript(DockerImageName image) {
        super(image);
        withCommand("-c", "while [ ! -f " + READY + " ]; do sleep 0.1; done; " + SCRIPT);
    }

    @Override
    protected void containerIsStarting(InspectContainerResponse containerInfo) {
        super.containerIsStarting(containerInfo);
        try {
            ExecResult result = execInContainer("touch", READY);
            if (result.getExitCode() != 0) {
                throw new IllegalStateException("touch " + READY + " failed: " + result.getStderr());
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(e);
        } catch (IOException e) {
            throw new IllegalStateException(e);
        }
    }
}
