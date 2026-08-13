package com.hometask.producer.service;

import com.hometask.producer.entity.MessageEntity;
import com.hometask.producer.exception.MessageAlreadyExistsException;
import com.hometask.producer.exception.MessageNotFoundException;
import com.hometask.producer.repository.MessageRepository;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

@Service
public class MessageService {

    private final MessageRepository messageRepository;

    public MessageService(MessageRepository messageRepository) {
        this.messageRepository = messageRepository;
    }

    @Retryable(
            retryFor = RuntimeException.class,
            noRetryFor = {MessageNotFoundException.class, MessageAlreadyExistsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${app.retry.delay-ms}")
    )
    public void create(int id, String msg) {
        if (messageRepository.existsById(id)) {
            throw new MessageAlreadyExistsException("Message already exists with id=" + id);
        }
        messageRepository.save(new MessageEntity(id, msg));
    }

    @Retryable(
            retryFor = RuntimeException.class,
            noRetryFor = {MessageNotFoundException.class, MessageAlreadyExistsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${app.retry.delay-ms}")
    )
    public void update(int id, String msg) {
        if (!messageRepository.existsById(id)) {
            throw new MessageNotFoundException("Message not found with id=" + id);
        }
        messageRepository.save(new MessageEntity(id, msg));
    }

    @Retryable(
            retryFor = RuntimeException.class,
            noRetryFor = {MessageNotFoundException.class, MessageAlreadyExistsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${app.retry.delay-ms}")
    )
    public void delete(int id) {
        if (!messageRepository.existsById(id)) {
            throw new MessageNotFoundException("Message not found with id=" + id);
        }
        messageRepository.deleteById(id);
    }

    @Retryable(
            retryFor = RuntimeException.class,
            noRetryFor = {MessageNotFoundException.class, MessageAlreadyExistsException.class},
            maxAttemptsExpression = "${app.retry.max-attempts}",
            backoff = @Backoff(delayExpression = "${app.retry.delay-ms}")
    )
    public MessageEntity read(int id) {
        return messageRepository.findById(id)
                .orElseThrow(() -> new MessageNotFoundException("Message not found with id=" + id));
    }
}
