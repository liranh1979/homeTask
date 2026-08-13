package com.hometask.server.service;

import com.hometask.server.dto.MessageResult;
import org.springframework.stereotype.Component;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

@Component
public class PendingReplyRegistry {

    private final ConcurrentHashMap<String, CompletableFuture<MessageResult>> pending = new ConcurrentHashMap<>();

    public CompletableFuture<MessageResult> register(String correlationId) {
        CompletableFuture<MessageResult> future = new CompletableFuture<>();
        pending.put(correlationId, future);
        return future;
    }

    public void complete(MessageResult result) {
        CompletableFuture<MessageResult> future = pending.remove(result.getCorrelationId());
        if (future != null) {
            future.complete(result);
        }
    }

    public void remove(String correlationId) {
        pending.remove(correlationId);
    }
}
