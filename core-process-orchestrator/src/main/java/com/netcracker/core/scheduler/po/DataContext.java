package com.netcracker.core.scheduler.po;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.netcracker.core.scheduler.po.repository.ContextRepository;
import com.netcracker.core.scheduler.po.repository.VersionMismatchException;
import com.netcracker.core.scheduler.po.serializers.DataContextDeserializer;
import com.netcracker.core.scheduler.po.serializers.DataContextSerializer;
import lombok.Getter;
import lombok.Setter;

import java.util.HashMap;
import java.util.function.Consumer;

@Getter
@JsonSerialize(using = DataContextSerializer.class)
@JsonDeserialize(using = DataContextDeserializer.class)
// Content equality inherited from HashMap is intentional: id, version, and the
// dirty flag are persistence bookkeeping, not part of the context's identity.
@SuppressWarnings("java:S2160")
public class DataContext extends HashMap<String, Object> {

    @Setter
    @JsonIgnore
    private transient ContextRepository repository;

    @Setter
    private transient boolean isDirty;
    private Integer version = 0;
    @Setter
    @JsonIgnore
    private String id;

    @JsonCreator
    public DataContext(@JsonProperty("id") String id) {
        isDirty = false;
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

    /**
     * Applies the mutation and saves. On a version conflict the fresh context is
     * reloaded, the same mutation is re-applied to it, and the fresh copy is
     * saved — so a fixed-value update (task state description, start/end time)
     * survives a concurrent writer instead of aborting the whole execution.
     * The mutation must be idempotent.
     */
    public void apply(Consumer<DataContext> function) {
        function.accept(this);
        try {
            save();
        } catch (VersionMismatchException e) {
            DataContext fresh = repository.getContext(getId());
            fresh.setRepository(repository);
            function.accept(fresh);
            fresh.save();
            // Sync this instance with the persisted state: contents, version, and
            // a clean dirty flag — otherwise the caller keeps a stale copy and the
            // very next save() fails again despite apply() having succeeded.
            // super-level access bypasses the dirty-marking overrides.
            super.clear();
            for (Entry<String, Object> entry : fresh.entrySet()) {
                super.put(entry.getKey(), entry.getValue());
            }
            setVersion(fresh.getVersion());
        }
    }
}
