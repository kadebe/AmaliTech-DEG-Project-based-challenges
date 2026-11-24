package com.finsafe.idempotencygateway.controller;

import com.finsafe.idempotencygateway.model.IdempotencyRecord;
import com.finsafe.idempotencygateway.model.PaymentRequest;
import com.finsafe.idempotencygateway.service.IdempotencyService;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import tools.jackson.databind.json.JsonMapper;  // Use JsonMapper

import java.util.concurrent.ExecutionException;

@RestController
public class PaymentController {

    private final IdempotencyService idempotencyService;
    private final JsonMapper jsonMapper = JsonMapper.builder().build();  // Create once

    public PaymentController(IdempotencyService idempotencyService) {
        this.idempotencyService = idempotencyService;
    }

    @PostMapping("/process-payment")
    public ResponseEntity<String> processPayment(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody String rawBody) {

        if (key == null || key.isBlank()) {
            return ResponseEntity.badRequest().build();
        }

        try {
            PaymentRequest request = jsonMapper.readValue(rawBody, PaymentRequest.class);
            IdempotencyRecord result = idempotencyService.processWithIdempotency(key, rawBody, request);

            HttpHeaders headers = new HttpHeaders();
            headers.add("Content-Type", "application/json");

            // New code (returns 200 for everything)
            if (result.getStatusCode() == 200) {
                headers.add("X-Cache-Hit", "true");
            }
            return ResponseEntity.ok().headers(headers).body(result.getResponseBody());


        } catch (IdempotencyService.ConflictException e) {
            return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
                    .body("{\"error\":\"" + e.getMessage() + "\"}");
        } catch (InterruptedException | ExecutionException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("{\"error\":\"Internal Server Error\"}");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("{\"error\":\"Invalid Request Body\"}");
        }
    }
}