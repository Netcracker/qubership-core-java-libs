package org.qubership.cloud.quarkus.consul.client.model;

import com.google.gson.annotations.SerializedName;

import java.nio.charset.Charset;
import java.util.Base64;

public class GetValue {

    private static final Charset UTF_8 = Charset.forName("UTF-8");

    @SerializedName("CreateIndex")
    private long createIndex;

    @SerializedName("ModifyIndex")
    private long modifyIndex;

    @SerializedName("LockIndex")
    private Long lockIndex;

    @SerializedName("Flags")
    private long flags;

    @SerializedName("Session")
    private String session;

    @SerializedName("Key")
    private String key;

    @SerializedName("Value")
    private String value;

    public String getDecodedValue(Charset charset) {
        if (value == null) {
            return null;
        }
        if (charset == null) {
            charset = UTF_8;
        }
        return new String(Base64.getDecoder().decode(value), charset);
    }

    public String getDecodedValue() {
        return getDecodedValue(UTF_8);
    }

    public long getCreateIndex() {
        return createIndex;
    }

    public long getModifyIndex() {
        return modifyIndex;
    }

    public Long getLockIndex() {
        return lockIndex;
    }

    public long getFlags() {
        return flags;
    }

    public String getSession() {
        return session;
    }

    public String getKey() {
        return key;
    }

    public String getValue() {
        return value;
    }

    public void setCreateIndex(long createIndex) {
        this.createIndex = createIndex;
    }

    public void setModifyIndex(long modifyIndex) {
        this.modifyIndex = modifyIndex;
    }

    public void setFlags(long flags) {
        this.flags = flags;
    }

    public void setLockIndex(Long lockIndex) {
        this.lockIndex = lockIndex;
    }

    public void setSession(String session) {
        this.session = session;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return "GetValue{" +
                "createIndex=" + createIndex +
                ", modifyIndex=" + modifyIndex +
                ", lockIndex=" + lockIndex +
                ", flags=" + flags +
                ", session='" + session + '\'' +
                ", key='" + key + '\'' +
                ", value='" + value + '\'' +
                '}';
    }
}