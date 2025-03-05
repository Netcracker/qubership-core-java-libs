package org.qubership.cloud.core.error.runtime;

public interface ErrorCode {
    /**
     *
     * @return error code in the format: [code]-[XХXX], cannot be null
     * where:
     * [code] - Application, Service or Library abbreviation in uppercase with delimiter '-'
     * [XXXX] - digits, padded with zeros on the left (0034, 0140, ...), may be 5 digits long
     */
    String getCode();

    /**
     *
     * @return error title. Message without secure detail and without error context information, cannot be null
     * The title does not change from one error instance to another when they both have the same Code
     */
    String getTitle();
}
