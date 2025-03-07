package org.qubership.cloud.maas.client.impl.dto.conf;

import lombok.Data;

/**
 * represents response from server.
 * Check status field value:
 *   - `ok' - operation successfuly executed, get data from `data' field
 *   - `error' - operation failed, get error description from `error' field
 */
@Data
public class ConfigureResult<T> {
    String status;
    String error;
    T data;
}
