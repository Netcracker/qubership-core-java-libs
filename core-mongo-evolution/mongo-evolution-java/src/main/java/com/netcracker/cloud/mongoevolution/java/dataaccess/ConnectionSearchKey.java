package org.qubership.cloud.mongoevolution.java.dataaccess;

import lombok.Value;

/**
 * ConnectionSearchKey uniquely identify tenant's database
 */
@Value
public class ConnectionSearchKey {
    String tenantId;
    String dbClassifier;
}
