package com.finsafe.idempotencygateway.model;


import java.time.LocalDateTime;

public class IdempotencyRecord {
    private String idempotencyKey;
    private String payloadHash;
    private String responseBody;
    private int statusCode;
    private LocalDateTime createdAt;

    public IdempotencyRecord(String key, String hash, String responseBody, int statusCode) {
        this.idempotencyKey = key;
        this.payloadHash = hash;
        this.responseBody = responseBody;
        this.statusCode = statusCode;
        this.createdAt = LocalDateTime.now();
    }

    public String getIdempotencyKey() { return idempotencyKey; }
    public String getPayloadHash() { return payloadHash; }
    public String getResponseBody() { return responseBody; }
    public int getStatusCode() { return statusCode; }
    public LocalDateTime getCreatedAt() { return createdAt; }
}