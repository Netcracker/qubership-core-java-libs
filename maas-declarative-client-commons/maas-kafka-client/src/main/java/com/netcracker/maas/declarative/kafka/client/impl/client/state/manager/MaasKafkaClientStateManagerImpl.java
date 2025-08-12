package com.netcracker.maas.declarative.kafka.client.impl.client.state.manager;

import com.netcracker.maas.declarative.kafka.client.api.*;
import com.netcracker.maas.declarative.kafka.client.impl.client.MaasKafkaInternalScheduledSystemExecServiceKeeper;
import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;
import com.netcracker.maas.declarative.kafka.client.impl.common.constant.MaasKafkaCommonConstants;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;


public class MaasKafkaClientStateManagerImpl implements MaasKafkaClientStateManagerService {

    private static final Logger LOG = LoggerFactory.getLogger(MaasKafkaClientStateManagerImpl.class);

    private final MaasKafkaClientStateChangeNotificationService notificationService;
    private final List<MaasKafkaClient> maasKafkaClients;

    private final ScheduledExecutorService scheduledExecutorService = MaasKafkaInternalScheduledSystemExecServiceKeeper.getExecutorService();

    public MaasKafkaClientStateManagerImpl(
            MaasKafkaClientStateChangeNotificationService notificationService,
            List<MaasKafkaClient> maasKafkaClients
    ) {
        this.notificationService = notificationService;
        this.maasKafkaClients = maasKafkaClients;
    }


    @Override
    public synchronized void emitClientActivationEvent() {
        notificationService.notifyOnActivationEvent();
        LOG.debug("Activation event has been emitted");
    }

    @Override
    public synchronized void emitClientDeactivationEvent() {
        notificationService.notifyOnDeactivationEvent();
        LOG.debug("Deactivation event has been emitted");
    }

    @Override
    public synchronized void activateInitializedClients(BiConsumer<MaasKafkaClient, CompletableFuture<Void>> afterActivationCallback) {
        activateClientsAfterInitialization(afterActivationCallback);
    }

    private void activateClientsAfterInitialization(BiConsumer<MaasKafkaClient, CompletableFuture<Void>> afterActivationCallback) {
        // use common execution queue to prevent call from a lot of threads
        scheduledExecutorService.submit(() -> {
            List<MaasKafkaClient> rmClients = null;
            for (MaasKafkaClient client : maasKafkaClients) {
                if (client.getClientState().equals(MaasKafkaClientState.INITIALIZED)) {
                    if (afterActivationCallback != null) {
                        afterActivationCallback.accept(client, client.activateAsync());
                    } else {
                        client.activateAsync()
                                .handle((v, e) -> {
                                    if (e != null) {
                                        String errMessage;
                                        if (client instanceof MaasKafkaConsumer) {
                                            errMessage = String.format(
                                                    "Activation failed for MaasKafkaConsumer %s due to exception",
                                                    ((MaasKafkaConsumer) client).getDefinition()
                                            );
                                        } else {
                                            // TODO add if/else
                                            errMessage = String.format(
                                                    "Activation failed for MaasKafkaProducer  %s due to exception",
                                                    ((MaasKafkaProducer) client).getDefinition()
                                            );
                                        }
                                        LOG.error(errMessage, e);
                                    } else {
                                        String successMessage;
                                        if (client instanceof MaasKafkaConsumer) {
                                            successMessage = String.format(
                                                    "Activation successfully completed for MaasKafkaConsumer %s",
                                                    ((MaasKafkaConsumer) client).getDefinition()
                                            );
                                        } else {
                                            // TODO add if/else
                                            successMessage = String.format(
                                                    "Activation successfully completed for MaasKafkaProducer %s",
                                                    ((MaasKafkaProducer) client).getDefinition()
                                            );
                                        }
                                        LOG.info(successMessage);
                                    }
                                    return null;
                                });
                    }
                    if (rmClients == null) {
                        rmClients = new ArrayList<>();
                    }
                    rmClients.add(client);
                } else if (client.getClientState().equals(MaasKafkaClientState.ACTIVE)
                        || client.getClientState().equals(MaasKafkaClientState.INACTIVE)) {
                    if (rmClients == null) {
                        rmClients = new ArrayList<>();
                    }
                    rmClients.add(client);
                }
            }

            if (rmClients != null) {
                maasKafkaClients.removeAll(rmClients);
            }

            if (!maasKafkaClients.isEmpty()) {
                scheduledExecutorService.schedule(
                        () -> activateInitializedClients(afterActivationCallback),
                        MaasKafkaCommonConstants.RETRY_ACTIVATION_TIMEOUT,
                        TimeUnit.MILLISECONDS
                );
            }
        });
    }
}
