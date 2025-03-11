package org.qubership.cloud.quarkus.consul.client.http;

public final class Response<T> {

    private final T value;

    private final Long consulIndex;
    private final Boolean consulKnownLeader;
    private final Long consulLastContact;

    public Response(T value, Long consulIndex, Boolean consulKnownLeader, Long consulLastContact) {
        this.value = value;
        this.consulIndex = consulIndex;
        this.consulKnownLeader = consulKnownLeader;
        this.consulLastContact = consulLastContact;
    }

    public T getValue() {
        return value;
    }

    public Long getConsulIndex() {
        return consulIndex;
    }

    public Boolean getConsulKnownLeader() {
        return consulKnownLeader;
    }

    public Long getConsulLastContact() {
        return consulLastContact;
    }

    @Override
    public String toString() {
        return "Response{" +
                "value=" + value +
                ", consulIndex=" + consulIndex +
                ", consulKnownLeader=" + consulKnownLeader +
                ", consulLastContact=" + consulLastContact +
                '}';
    }
}