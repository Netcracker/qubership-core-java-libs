package com.netcracker.core.scheduler.po;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netcracker.core.scheduler.po.repository.ContextRepository;
import com.netcracker.core.scheduler.po.serializers.DataContextDeserializer;
import com.netcracker.core.scheduler.po.serializers.DataContextSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.function.Consumer;

@Getter
@JsonSerialize(using = DataContextSerializer.class)
@JsonDeserialize(using = DataContextDeserializer.class)
public class DataContext extends HashMap<String, Object> {

    @Setter
    @JsonIgnore
    private transient ContextRepository repository;

    @Setter
    private transient boolean isDirty;
    private Integer version = 0;
    @JsonIgnore
    private String id;

    @JsonCreator
    public DataContext(@JsonProperty("id") String id) {
        isDirty = false;
        this.id = id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setVersion(Integer version) {
        this.version = version;
        isDirty = false;
    }

    @Override
    public Object put(String key, Object value) {
        if (!isDirty) {
            isDirty = true;
        }
        if (value == null) return remove(key);
        else
            return super.put(key, value);
    }

    @Override
    public Object remove(Object key) {
        if (!isDirty) {
            isDirty = true;
        }
        return super.remove(key);
    }

    @Override
    public void clear() {
        if (!isDirty) {
            isDirty = true;
        }
        super.clear();
    }

    public void save() {
        repository.putContext(this);
    }

    public void apply(Consumer<DataContext> function) {
        function.accept(this);
        save();
    }

    @Override
    public boolean equals(Object o) {
        return super.equals(o);
    }

    @Override
    public int hashCode() {
        return super.hashCode();
    }
}
