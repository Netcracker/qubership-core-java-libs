package org.qubership.maas.declarative.kafka.client.impl.client.notification.api;

// TODO platform dependent service(or make common)
public interface MaasKafkaClientStateChangeNotificationService {

    void notifyOnActivationEvent();

    void notifyOnDeactivationEvent();

    void subscribeOnActivationEvent(Runnable callback);

    void subscribeOnDeactivationEvent(Runnable callback);

}
