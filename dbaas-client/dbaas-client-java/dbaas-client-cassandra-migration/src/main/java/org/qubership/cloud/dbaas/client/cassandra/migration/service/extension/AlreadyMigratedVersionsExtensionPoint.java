package org.qubership.cloud.dbaas.client.cassandra.migration.service.extension;

import com.datastax.oss.driver.api.core.CqlSession;
import org.qubership.cloud.dbaas.client.cassandra.migration.model.AlreadyMigratedVersion;

import java.util.List;

/**
 * Provides information about schema versions that were already migrated before library usage
 */
public interface AlreadyMigratedVersionsExtensionPoint {
    /**
     * Method provides the list of schema versions that were already migrated before library usage.
     * Method will be used by library only if the library owned versioning table is empty.
     * Versions provided by the method will be inserted in the library owned versioning table as if they were migrated by library
     * and then will be used for changeset calculation.
     *
     * @param session session where schema migration is executed. Can be used e.g. to read previous versioning table.
     * @return already migrated schema versions list
     */
    List<AlreadyMigratedVersion> getAlreadyMigratedVersions(CqlSession session);
}
