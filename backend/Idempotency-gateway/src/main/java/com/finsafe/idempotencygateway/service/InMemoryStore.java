package com.finsafe.idempotencygateway.service;


import com.finsafe.idempotencygateway.model.IdempotencyRecord;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;

@Component
public class InMemoryStore {
    private final ConcurrentHashMap<String, IdempotencyRecord> store = new ConcurrentHashMap<>();

    public IdempotencyRecord get(String key) {
        return store.get(key);
    }

    public void save(String key, IdempotencyRecord record) {
        store.put(key, record);
    }

    public void remove(String key) {
        store.remove(key);
    }
}