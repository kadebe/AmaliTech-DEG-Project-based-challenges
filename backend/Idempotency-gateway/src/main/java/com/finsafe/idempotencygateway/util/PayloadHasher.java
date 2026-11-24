package com.finsafe.idempotencygateway.util;


import org.springframework.stereotype.Component;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.TreeMap;

@Component
public class PayloadHasher {
    private final ObjectMapper objectMapper = new ObjectMapper();

    public String computeHash(String rawJson) throws Exception {
        JsonNode node = objectMapper.readTree(rawJson);
        TreeMap<String, Object> sorted = objectMapper.convertValue(node, TreeMap.class);
        String canonical = objectMapper.writeValueAsString(sorted);

        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] digest = md.digest(canonical.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().formatHex(digest);
    }
}