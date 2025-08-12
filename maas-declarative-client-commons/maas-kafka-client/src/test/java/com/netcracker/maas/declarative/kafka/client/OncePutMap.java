package com.netcracker.maas.declarative.kafka.client;

import java.util.HashMap;

// for idempotency testing
public class OncePutMap<K, V> extends HashMap<K, V> {

    @Override
    public V put(K key, V value) {
        if (this.containsKey(key)) {
            throw new RuntimeException("Putting for key: " + key + " can be called only once");
        }
        return super.put(key, value);
    }
}
