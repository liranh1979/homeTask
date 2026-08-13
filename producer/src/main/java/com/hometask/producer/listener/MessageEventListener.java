package com.hometask.producer.listener;

import com.hometask.producer.dto.Message;
import com.hometask.producer.dto.MessageResult;
import com.hometask.producer.entity.MessageEntity;
import com.hometask.producer.exception.MessageAlreadyExistsException;
import com.hometask.producer.exception.MessageNotFoundException;
import com.hometask.producer.service.MessageReplyService;
import com.hometask.producer.service.MessageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageEventListener {

    private static final Logger log = LoggerFactory.getLogger(MessageEventListener.class);

    private final MessageService messageService;
    private final MessageReplyService messageReplyService;

    public MessageEventListener(MessageService messageService, MessageReplyService messageReplyService) {
        this.messageService = messageService;
        this.messageReplyService = messageReplyService;
    }

    @KafkaListener(topics = "${kafka.topic.create}", groupId = "${spring.kafka.consumer.group-id}")
    public void onCreate(Message message) {
        log.info("CREATE event received: {}", message);
        try {
            messageService.create(message.getId(), message.getMsg());
            messageReplyService.send(MessageResult.success(message.getCorrelationId(), message));
        } catch (Exception e) {
            handleFailure("CREATE", message, e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.update}", groupId = "${spring.kafka.consumer.group-id}")
    public void onUpdate(Message message) {
        log.info("UPDATE event received: {}", message);
        try {
            messageService.update(message.getId(), message.getMsg());
            messageReplyService.send(MessageResult.success(message.getCorrelationId(), message));
        } catch (Exception e) {
            handleFailure("UPDATE", message, e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.delete}", groupId = "${spring.kafka.consumer.group-id}")
    public void onDelete(Message message) {
        log.info("DELETE event received: {}", message);
        try {
            messageService.delete(message.getId());
            messageReplyService.send(MessageResult.success(message.getCorrelationId(), null));
        } catch (Exception e) {
            handleFailure("DELETE", message, e);
        }
    }

    @KafkaListener(topics = "${kafka.topic.read}", groupId = "${spring.kafka.consumer.group-id}")
    public void onRead(Message message) {
        log.info("READ event received: {}", message);
        try {
            MessageEntity found = messageService.read(message.getId());
            Message data = new Message(found.getId(), found.getMsg());
            messageReplyService.send(MessageResult.success(message.getCorrelationId(), data));
        } catch (Exception e) {
            handleFailure("READ", message, e);
        }
    }

    private void handleFailure(String operation, Message message, Exception e) {
        String errorCode;
        if (e instanceof MessageNotFoundException) {
            errorCode = "NOT_FOUND";
        } else if (e instanceof MessageAlreadyExistsException) {
            errorCode = "ALREADY_EXISTS";
        } else {
            errorCode = "INTERNAL_ERROR";
            log.error("{} failed for id={}", operation, message.getId(), e);
        }
        messageReplyService.send(MessageResult.failure(message.getCorrelationId(), errorCode, e.getMessage()));
    }
}
