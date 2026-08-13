package com.hometask.server.listener;

import com.hometask.server.dto.MessageResult;
import com.hometask.server.service.PendingReplyRegistry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;

@Component
public class MessageReplyListener {

    private static final Logger log = LoggerFactory.getLogger(MessageReplyListener.class);

    private final PendingReplyRegistry pendingReplyRegistry;

    public MessageReplyListener(PendingReplyRegistry pendingReplyRegistry) {
        this.pendingReplyRegistry = pendingReplyRegistry;
    }

    @KafkaListener(topics = "${kafka.topic.reply}", groupId = "${spring.kafka.consumer.group-id}")
    public void onReply(MessageResult result) {
        log.info("Reply received: {}", result);
        pendingReplyRegistry.complete(result);
    }
}
