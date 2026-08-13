package com.hometask.server.service;

import com.hometask.server.dto.Message;
import com.hometask.server.dto.MessageResult;
import com.hometask.server.exception.KafkaReplyTimeoutException;
import com.hometask.server.exception.MessageProcessingException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Service
public class MessageProducerService {

    private final KafkaTemplate<String, Message> kafkaTemplate;
    private final PendingReplyRegistry pendingReplyRegistry;

    @Value("${kafka.topic.create}")
    private String createTopic;

    @Value("${kafka.topic.update}")
    private String updateTopic;

    @Value("${kafka.topic.delete}")
    private String deleteTopic;

    @Value("${kafka.topic.read}")
    private String readTopic;

    @Value("${kafka.reply.timeout-seconds}")
    private long replyTimeoutSeconds;

    public MessageProducerService(KafkaTemplate<String, Message> kafkaTemplate, PendingReplyRegistry pendingReplyRegistry) {
        this.kafkaTemplate = kafkaTemplate;
        this.pendingReplyRegistry = pendingReplyRegistry;
    }

    public MessageResult sendCreate(Message message) {
        return sendAndAwait(createTopic, message);
    }

    @CacheEvict(value = "messages", key = "#message.id", beforeInvocation = true)
    public MessageResult sendUpdate(Message message) {
        return sendAndAwait(updateTopic, message);
    }

    @CacheEvict(value = "messages", key = "#message.id", beforeInvocation = true)
    public MessageResult sendDelete(Message message) {
        return sendAndAwait(deleteTopic, message);
    }

    @Cacheable(value = "messages", key = "#message.id", unless = "#result == null || !#result.success")
    public MessageResult sendRead(Message message) {
        return sendAndAwait(readTopic, message);
    }

    private MessageResult sendAndAwait(String topic, Message message) {
        String correlationId = UUID.randomUUID().toString();
        message.setCorrelationId(correlationId);

        CompletableFuture<MessageResult> future = pendingReplyRegistry.register(correlationId);
        kafkaTemplate.send(topic, String.valueOf(message.getId()), message);

        try {
            return future.get(replyTimeoutSeconds, TimeUnit.SECONDS);
        } catch (TimeoutException e) {
            throw new KafkaReplyTimeoutException("Timed out waiting for confirmation of message id=" + message.getId());
        } catch (ExecutionException e) {
            throw new MessageProcessingException("Failed to process message id=" + message.getId());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new MessageProcessingException("Interrupted while waiting for confirmation of message id=" + message.getId());
        } finally {
            pendingReplyRegistry.remove(correlationId);
        }
    }
}
