package org.qubership.cloud.quarkus.consul.client.http;

public final class OperationException extends RuntimeException {

    private final int statusCode;
    private final String statusMessage;

    public int getStatusCode() {
        return statusCode;
    }

    public String getStatusMessage() {
        return statusMessage;
    }

    public String getStatusContent() {
        return statusContent;
    }

    private final String statusContent;

    public OperationException(int statusCode, String statusMessage, String statusContent) {
        super("OperationException(statusCode=" + statusCode + ", statusMessage='" + statusMessage + "', statusContent='" + statusContent + "')");
        this.statusCode = statusCode;
        this.statusMessage = statusMessage;
        this.statusContent = statusContent;
    }

    @Override
    public String toString() {
        return "OperationException{" +
                "statusCode=" + statusCode +
                ", statusMessage='" + statusMessage + '\'' +
                ", statusContent='" + statusContent + '\'' +
                '}';
    }
}
