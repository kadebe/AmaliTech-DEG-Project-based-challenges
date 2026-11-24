package com.finsafe.idempotencygateway.service;

import com.finsafe.idempotencygateway.model.IdempotencyRecord;
import com.finsafe.idempotencygateway.model.PaymentRequest;
import com.finsafe.idempotencygateway.util.PayloadHasher;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

@Service
public class IdempotencyService {

    private final InMemoryStore store;
    private final PayloadHasher hasher;
    private final InFlightManager inFlightManager;

    public IdempotencyService(InMemoryStore store, PayloadHasher hasher, InFlightManager inFlightManager) {
        this.store = store;
        this.hasher = hasher;
        this.inFlightManager = inFlightManager;
    }

    public static class ConflictException extends RuntimeException {
        public ConflictException(String msg) { super(msg); }
    }

    public IdempotencyRecord processWithIdempotency(String key, String rawBody, PaymentRequest request)
            throws InterruptedException, ExecutionException {

        String hash;
        try {
            hash = hasher.computeHash(rawBody);
        } catch (Exception e) {
            throw new RuntimeException("Hash error");
        }

        // 1. Check Memory Store
        IdempotencyRecord existing = store.get(key);

        if (existing != null) {
            // Check TTL (24 hours)
            if (existing.getCreatedAt().isAfter(LocalDateTime.now().minusHours(24))) {

                // Check Hash Match
                if (existing.getPayloadHash().equals(hash)) {
                    // 🔥 CHANGE THIS LINE: Return a new record with status code 200
                    return new IdempotencyRecord(key, hash, existing.getResponseBody(), 200);
                } else {
                    throw new ConflictException("Idempotency key already used for a different request body."); // Conflict 422
                }
            } else {
                // Expired, remove and proceed as new
                store.remove(key);
            }
        }

        // 2. Check In-Flight Race (CompletableFuture)
        CompletableFuture<String> activePromise = inFlightManager.getActive(key);
        if (activePromise != null) {
            // Await active thread
            String completedResponse = activePromise.join();
            // Return a constructed record indicating cache hit (already 200)
            return new IdempotencyRecord(key, hash, completedResponse, 200);
        }

        // 3. Primary Processing Execution
        CompletableFuture<String> myPromise = inFlightManager.register(key);

        try {
            // Execute Payment (Simulated 2s Latency)
            Thread.sleep(2000);
            String responseBody = "{\"status\":\"Charged " + request.getAmount() + " " + request.getCurrency() + "\"}";

            // Save IdempotencyRecord to In-Memory Store (status 201)
            IdempotencyRecord newRecord = new IdempotencyRecord(key, hash, responseBody, 201);
            store.save(key, newRecord);

            // Complete In-Flight Promise & Evict Lock
            inFlightManager.complete(key, responseBody);

            return newRecord;
        } catch (Exception e) {
            inFlightManager.complete(key, "{\"error\":\"Payment Failed\"}");
            throw e;
        }
    }
}