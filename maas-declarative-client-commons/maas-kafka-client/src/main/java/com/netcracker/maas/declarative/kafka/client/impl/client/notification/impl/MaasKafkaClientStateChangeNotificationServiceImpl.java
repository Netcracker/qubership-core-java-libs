package com.netcracker.maas.declarative.kafka.client.impl.client.notification.impl;

import com.netcracker.maas.declarative.kafka.client.impl.client.notification.api.MaasKafkaClientStateChangeNotificationService;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;


public class MaasKafkaClientStateChangeNotificationServiceImpl implements MaasKafkaClientStateChangeNotificationService {

    private final List<Runnable> activationEventCallbacks = new ArrayList<>();
    private final List<Runnable> deactivationEventCallbacks = new ArrayList<>();

    @Override
    public void notifyOnActivationEvent() {
        callForSyncForList(activationEventCallbacks, list -> {
            for (Runnable runnable : list) {
                runnable.run();
            }
            return null;
        });
    }

    @Override
    public void notifyOnDeactivationEvent() {
        callForSyncForList(deactivationEventCallbacks, list -> {
            for (Runnable runnable : list) {
                runnable.run();
            }
            return null;
        });
    }

    @Override
    public void subscribeOnActivationEvent(Runnable callback) {
        callForSyncForList(activationEventCallbacks, list -> {
            list.add(callback);
            return null;
        });
    }

    @Override
    public void subscribeOnDeactivationEvent(Runnable callback) {
        callForSyncForList(deactivationEventCallbacks, list -> {
            list.add(callback);
            return null;
        });

    }

    private synchronized void callForSyncForList(List<Runnable> list, Function<List<Runnable>, Void> func) {
        func.apply(list);
    }
}
