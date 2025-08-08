package com.netcracker.cloud.maas.client.api;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.ObjectCodec;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;
import org.qubership.cloud.maas.client.impl.Env;

import java.io.IOException;
import java.util.*;

@JsonSerialize(using = Classifier.ClassifierSerializer.class)
@JsonDeserialize(using = Classifier.ClassifierDeserializer.class)
public class Classifier {
    public static final String NAME = "name";
    public static final String TENANT_ID = "tenantId";
    public static final String NAMESPACE = "namespace";

    // intentionally simplify structure to map of string -> string, not string -> object
    // and inentionally use LinkedHashMap to prevent key/value pairs resorting: name key should be always first
    private final Map<String, String> classifier;

    /**
     * Generic purpose constrictor
     * @param name mandatory name value
     * @param opts optional key-value pairs, must be even number of arguments
     */
    public Classifier(String name, String... opts) {
        if (name == null || name.length() == 0) {
            throw new IllegalArgumentException("name field should be non empty string");
        }

        if (opts.length % 2 != 0) {
            throw new IllegalArgumentException("opts must be even pairs of `name' and `value'");
        }

        var coll = new HashMap<String, String>(3);
        coll.put(NAME, name);
        for (int i = 0; i < opts.length; i += 2) {
            coll.put(opts[i], opts[i + 1]);
        }

        this.classifier = transferFromMap(coll);
    }

    /**
     * Instantiate classifier using only relevant key-value pairs from map
     * @param map map with mandatory name, namespace and optional tenantId keys
     */
    public Classifier(Map<String, String> map) {
        this.classifier = transferFromMap(map);
    }

    private static Map<String, String> transferFromMap(Map<String, String> map) {
        var result = new LinkedHashMap<String, String>(3);
        Optional.ofNullable(map.get(NAME))
                .ifPresentOrElse(
                        value -> result.put(NAME, value),
                        () -> { throw new IllegalArgumentException("Can't create classifier from map: value for mandatory key `" + NAME + "' not found"); }
                );

        Optional.ofNullable(map.get(TENANT_ID))
                .ifPresent(value -> result.put(TENANT_ID, value));

        Optional.ofNullable(map.get(NAMESPACE))
                .ifPresentOrElse(
                        value -> result.put(NAMESPACE, value),
                        () -> result.put(NAMESPACE, Env.namespace())
                );
        return result;
    }

    /**
     * Get name value from classifier
     * @return name
     */
    public String getName() {
        return classifier.get(NAME);
    }

    /**
     * Override namespace value
     * @param namespace namespace name
     * @return updated classifier
     */
    public Classifier namespace(String namespace) {
        classifier.put(NAMESPACE, namespace);
        return this;
    }

    /**
     * Get namespace value from classifier
     * @return name of namespace
     */
    public String getNamespace() {
        return classifier.get(NAMESPACE);
    }

    /**
     * Tenant id string
     * @param tenantId UUID as string
     * @return classifier instance
     */
    public Classifier tenantId(String tenantId) {
        classifier.put(TENANT_ID, tenantId);
        return this;
    }

    /**
     * Get tenant id from classifier
     * @return its optional value, so it can be empty for non tenant entities
     */
    public Optional<String> getTenantId() {
        return Optional.ofNullable(classifier.get(TENANT_ID));
    }

    /**
     * Get classifier values as map
     *
     * @return classifier in map structure representation
     */
    public Map<String, String> toMap() {
        return Collections.unmodifiableMap(classifier);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Classifier that = (Classifier) o;
        return classifier.equals(that.classifier);
    }

    @Override
    public int hashCode() {
        return Objects.hash(classifier);
    }

    @Override
    public String toString() {
        return "Classifier" + classifier;
    }

    static class ClassifierDeserializer extends StdDeserializer<Classifier> {
        public ClassifierDeserializer() {
            this(null);
        }

        public ClassifierDeserializer(Class<?> c) {
            super(c);
        }

        @Override
        public Classifier deserialize(JsonParser parser, DeserializationContext deserializationContext) throws IOException {
            ObjectCodec codec = parser.getCodec();

            TypeReference<Map<String, String>> typeRef = new TypeReference<Map<String, String>>() {};
            Map<String, String> map = codec.readValue(parser, typeRef);

            // validate classifier fields
            if (!map.containsKey(NAME)) {
                throw new IllegalArgumentException(
                        "Error deserialize classifier:`" + map + "', missed mandatory key: " + NAME);
            }
            if (!map.containsKey(NAMESPACE)) {
                throw new IllegalArgumentException(
                        "Error deserialize classifier:`" + map + "', missed mandatory key: " + NAMESPACE);
            }

            return new Classifier(map);
        }
    }

    static class ClassifierSerializer extends StdSerializer<Classifier> {
        public ClassifierSerializer() {
            this(null);
        }

        public ClassifierSerializer(Class<Classifier> t) {
            super(t);
        }

        @Override
        public void serialize(Classifier value, JsonGenerator jgen, SerializerProvider provider) throws IOException {
            jgen.writeStartObject();
            for (Map.Entry<String, String> entry : value.toMap().entrySet()) {
                jgen.writeStringField(entry.getKey(), entry.getValue());
            }
            jgen.writeEndObject();
        }
    }
}
