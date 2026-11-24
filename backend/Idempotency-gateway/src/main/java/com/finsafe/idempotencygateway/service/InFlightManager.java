package com.finsafe.idempotencygateway.service;


import org.springframework.stereotype.Component;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class InFlightManager {
    private final ConcurrentHashMap<String, CompletableFuture<String>> activePromises = new ConcurrentHashMap<>();

    public CompletableFuture<String> register(String key) {
        CompletableFuture<String> future = new CompletableFuture<>();
        activePromises.put(key, future);
        return future;
    }

    public CompletableFuture<String> getActive(String key) {
        return activePromises.get(key);
    }

    public void complete(String key, String response) {
        CompletableFuture<String> future = activePromises.remove(key);
        if (future != null) {
            future.complete(response);
        }
    }
}
