package org.qubership.cloud.maas.client.api.rabbit;

import org.qubership.cloud.bluegreen.api.service.BlueGreenStatePublisher;
import org.qubership.cloud.maas.client.api.Classifier;

import java.util.List;

public interface RabbitMaaSClient {
    /**
     * Find VHost by given name.
     * Internally VHoss will be searched by classifier with two fields: name and namespace.
     *
     * @param name name for vhost
     * @return VHost structure with connection properties
     *
     * @deprecated use {@link #getOrCreateVirtualHost(Classifier)}
     */
    @Deprecated
    VHost getVHost(String name);

    /**
     * Find VHost by given name  and tenant id.
     * Internally VHoss will be searched by classifier with three fields: name, tenantId and namespace.
     *
     * @param name name for vhost
     * @param tenantId id of tenant
     * @return VHost structure with connection properties
     *
     * @deprecated use {@link #getOrCreateVirtualHost(Classifier)}
     */
    @Deprecated
    VHost getVHost(String name, String tenantId);

    /**
     * Find VHost by classifier match
     *
     * @param classifier vhost classifier
     * @return VHost structure with connection properties
     * @deprecated use {@link #getOrCreateVirtualHost(Classifier)}
     *
     */
    @Deprecated
    default VHost getVHost(Classifier classifier) {
        throw new UnsupportedOperationException("Should be implemented in subclasses");
    };


    /**
     * Find VHost by classifier match
     *
     * @param classifier vhost classifier
     * @return VHost structure with connection properties and Rabbit configuration
     */

    default VHost getVirtualHost(Classifier classifier) {
        throw new UnsupportedOperationException("Should be implemented in subclasses");
    };


    /**
     * Get or Create VHost by classifier match
     *
     * @param classifier vhost classifier
     * @return VHost structure with connection properties
     *
     */

    default VHost getOrCreateVirtualHost(Classifier classifier) {
        throw new UnsupportedOperationException("Should be implemented in subclasses");
    };



    /**
     * Calculate versioned queue name using given name with value from environment DEPLOYMENT_VERSION variable.
     * <br><strong>
     * Warning! Ensure DEPLOYMENT_VERSION is set in application environment. Missing this variable can cause
     * wrong queue selection in blue/green deployment.<br>
     * Empty of missed DEPLOYMENT_VERSION assumed to `v1'
     * </strong>
     * @param queueName queue name concantenated with DEPLOYMENT_VERSION variable value via `-' char
     * @return returns string with queue name. For example, for queue name `foo' and DEPLOYMENT_VERSION
     *  env value `v2' resulting queue name will be `foo-v2'.

     */
    String getVersionedQueueName(String queueName, BlueGreenStatePublisher publisher);

    /**
     * Get real exchange names by business exchange name declared in configuration:
     * <ul>
     * <li>if exchange was declared as in non versioned section, then returned list will contain the same value as given parameter `exchangeName' </li>
     * <li>if requested exchange was declared in versionedEntites configuration section, then result will contain List of all created versioned exchanges</li>
     * </ul>
     * @param exchangeName buisiness exchange name
     * @return list of real exchange names
     */
    List<String> getExchangeNames(String exchangeName);
}
