package com.gasagency.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gasagency.entity.ApiIdempotencyRecord;
import com.gasagency.exception.InvalidOperationException;
import com.gasagency.repository.ApiIdempotencyRecordRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

@Service
public class ApiIdempotencyService {
    private final ApiIdempotencyRecordRepository repository;
    private final ObjectMapper objectMapper;

    public ApiIdempotencyService(ApiIdempotencyRecordRepository repository, ObjectMapper objectMapper) {
        this.repository = repository;
        this.objectMapper = objectMapper;
    }

    public <T> T execute(
            String endpoint,
            String idempotencyKey,
            String username,
            Object requestPayload,
            Supplier<T> createOperation,
            Function<T, Long> resourceIdExtractor,
            Function<Long, T> resourceFetcher
    ) {
        if (idempotencyKey == null || idempotencyKey.trim().isEmpty()) {
            return createOperation.get();
        }

        final String key = idempotencyKey.trim();
        final String actor = (username == null || username.isBlank()) ? "anonymous" : username;
        final String requestHash = hashPayload(requestPayload);

        ApiIdempotencyRecord record;
        Optional<ApiIdempotencyRecord> existingRecord = repository
                .findByIdempotencyKeyAndUsernameAndEndpoint(key, actor, endpoint);

        if (existingRecord.isPresent()) {
            record = existingRecord.get();
            validateRequestHash(record, requestHash);

            if (record.getStatus() == ApiIdempotencyRecord.Status.COMPLETED && record.getResourceId() != null) {
                return resourceFetcher.apply(record.getResourceId());
            }
            if (record.getStatus() == ApiIdempotencyRecord.Status.IN_PROGRESS) {
                throw new InvalidOperationException("This request is already being processed. Please wait and retry.");
            }

            // FAILED record: allow retry with same key and same payload.
            record.setStatus(ApiIdempotencyRecord.Status.IN_PROGRESS);
            record.setResourceId(null);
            repository.save(record);
        } else {
            record = new ApiIdempotencyRecord();
            record.setEndpoint(endpoint);
            record.setIdempotencyKey(key);
            record.setUsername(actor);
            record.setRequestHash(requestHash);
            record.setStatus(ApiIdempotencyRecord.Status.IN_PROGRESS);
            record.setResourceId(null);
            try {
                record = repository.save(record);
            } catch (DataIntegrityViolationException e) {
                ApiIdempotencyRecord concurrentRecord = repository
                        .findByIdempotencyKeyAndUsernameAndEndpoint(key, actor, endpoint)
                        .orElseThrow(() -> e);
                validateRequestHash(concurrentRecord, requestHash);
                if (concurrentRecord.getStatus() == ApiIdempotencyRecord.Status.COMPLETED
                        && concurrentRecord.getResourceId() != null) {
                    return resourceFetcher.apply(concurrentRecord.getResourceId());
                }
                throw new InvalidOperationException("This request is already being processed. Please wait and retry.");
            }
        }

        try {
            T created = createOperation.get();
            Long resourceId = resourceIdExtractor.apply(created);
            record.setStatus(ApiIdempotencyRecord.Status.COMPLETED);
            record.setResourceId(resourceId);
            repository.save(record);
            return created;
        } catch (RuntimeException ex) {
            record.setStatus(ApiIdempotencyRecord.Status.FAILED);
            repository.save(record);
            throw ex;
        }
    }

    private void validateRequestHash(ApiIdempotencyRecord record, String requestHash) {
        if (!record.getRequestHash().equals(requestHash)) {
            throw new InvalidOperationException(
                    "This idempotency key was already used with a different request payload.");
        }
    }

    private String hashPayload(Object payload) {
        String json;
        try {
            json = objectMapper.writeValueAsString(payload);
        } catch (JsonProcessingException e) {
            throw new InvalidOperationException("Unable to process request payload for idempotency.");
        }

        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(json.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(hash.length * 2);
            for (byte b : hash) {
                sb.append(String.format("%02x", b));
            }
            return sb.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new InvalidOperationException("Idempotency hashing algorithm not available.");
        }
    }
}
