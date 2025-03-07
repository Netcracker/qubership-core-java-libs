package org.qubership.cloud.dbaas.client.cassandra.migration.service.resource;

import java.io.IOException;
import java.net.URI;
import java.util.List;

/**
 * Provides the ability to find schema version resources inside resource holders.
 */
public interface SchemaVersionResourceFinder {
    /**
     * Method to find schema version resource names inside resource holder.
     *
     * @param resourceHolderUri resource holder URI
     * @param resourceLocation  resource location inside resource holder
     * @return resource names list
     * @throws IOException if thrown by implementations
     */
    List<String> findResourceNames(URI resourceHolderUri, String resourceLocation) throws IOException;
}
