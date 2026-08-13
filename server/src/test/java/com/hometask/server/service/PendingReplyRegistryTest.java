package com.hometask.server.service;

import com.hometask.server.dto.MessageResult;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;

class PendingReplyRegistryTest {

    private final PendingReplyRegistry registry = new PendingReplyRegistry();

    private static MessageResult successResult(String correlationId) {
        MessageResult result = new MessageResult();
        result.setCorrelationId(correlationId);
        result.setSuccess(true);
        return result;
    }

    @Test
    void registerReturnsIncompleteFuture() {
        CompletableFuture<MessageResult> future = registry.register("corr-1");

        assertThat(future).isNotDone();
    }

    @Test
    void completeResolvesTheMatchingFuture() {
        CompletableFuture<MessageResult> future = registry.register("corr-1");
        MessageResult result = successResult("corr-1");

        registry.complete(result);

        assertThat(future).isCompletedWithValue(result);
    }

    @Test
    void completeWithUnknownCorrelationIdIsANoop() {
        registry.complete(successResult("unknown-corr"));
    }

    @Test
    void completeOnlyResolvesTheMatchingCorrelationId() {
        CompletableFuture<MessageResult> futureA = registry.register("corr-a");
        CompletableFuture<MessageResult> futureB = registry.register("corr-b");

        registry.complete(successResult("corr-a"));

        assertThat(futureA).isDone();
        assertThat(futureB).isNotDone();
    }

    @Test
    void removeDropsThePendingEntrySoLaterCompletesAreIgnored() {
        CompletableFuture<MessageResult> future = registry.register("corr-1");

        registry.remove("corr-1");
        registry.complete(successResult("corr-1"));

        assertThat(future).isNotDone();
    }
}
