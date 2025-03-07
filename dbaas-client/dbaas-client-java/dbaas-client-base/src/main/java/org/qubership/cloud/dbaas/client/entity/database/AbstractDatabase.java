package org.qubership.cloud.dbaas.client.entity.database;

import org.qubership.cloud.dbaas.client.entity.connection.DatabaseConnection;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;

import java.util.Map;
import java.util.SortedMap;

import static org.qubership.cloud.dbaas.client.DbaasConst.*;


@Data
@Slf4j
public abstract class AbstractDatabase<P> implements AutoCloseable {

    private SortedMap<String, Object> classifier;
    private P connectionProperties;
    private String physicalDatabaseId;
    private String name;
    private String namespace;
    private Map<String, Object> settings;

    private boolean doClose;

    @Override
    public void close() throws Exception {
        if (this.getConnectionProperties() instanceof DatabaseConnection) {
            DatabaseConnection conn = (DatabaseConnection) this.getConnectionProperties();
            if (this.isDoClose()) {
                conn.close();
            }
        } else {
            log.error("Cannot close underlying connection, because it's not of DatabaseConnection type");
        }
    }

    public boolean isClassifierContainsLogicalDbName() {
        SortedMap<String, Object> classifier = getClassifier();
        return classifier != null && classifier.containsKey(CUSTOM_KEYS) && ((Map<String, String>) classifier.get(CUSTOM_KEYS)).containsKey(LOGICAL_DB_NAME);
    }

    public String toString() {
        return "AbstractDatabase(classifier=" + this.getClassifier()
                + ", connectionProperties=" + this.getConnectionProperties().toString().replaceAll("(.+://)(.+):(.+@)(.+)", "$1$2:***@$4")
                + ", physicalDatabaseId=" + this.getPhysicalDatabaseId()
                + ", name=" + this.getName()
                + ", namespace=" + this.getNamespace()
                + ", settings=" + this.getSettings()
                + ", doClose=" + this.isDoClose()
                + ")";
    }
}
