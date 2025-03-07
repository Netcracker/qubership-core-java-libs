package org.qubership.cloud.bluegreen.impl.dto.consul;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;

@JsonSerialize(using = TxnKVVerb.TxnKVVerbSerializer.class)
public enum TxnKVVerb {
    SET("set"),
    CAS("cas"),
    LOCK("lock"),
    UNLOCK("unlock"),
    GET("get"),
    GET_TREE("get-tree"),
    CHECK_INDEX("check-index"),
    CHECK_SESSION("check-session"),
    CHECK_NOT_EXISTS("check-not-exists"),
    DELETE("delete"),
    DELETE_TREE("delete-tree"),
    DELETE_CAS("delete-cas");

    public static TxnKVVerb ofVerb(String verb) {
        for (TxnKVVerb type : values()) {
            if (type.getVerb().equals(verb)) {
                return type;
            }
        }
        return null;
    }

    private final String verb;

    TxnKVVerb(String verb) {
        this.verb = verb;
    }

    public String getVerb() {
        return verb;
    }


    static class TxnKVVerbSerializer extends StdSerializer<TxnKVVerb> {

        public TxnKVVerbSerializer() {
            super(TxnKVVerb.class);
        }

        public TxnKVVerbSerializer(Class t) {
            super(t);
        }

        public void serialize(TxnKVVerb verb, JsonGenerator generator, SerializerProvider provider) throws IOException, JsonProcessingException {
            generator.writeString(verb.getVerb());
        }
    }
}
