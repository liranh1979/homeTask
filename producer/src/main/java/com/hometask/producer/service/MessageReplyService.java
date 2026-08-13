package com.hometask.producer.service;

import com.hometask.producer.dto.MessageResult;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
public class MessageReplyService {

    private final KafkaTemplate<String, MessageResult> kafkaTemplate;

    @Value("${kafka.topic.reply}")
    private String replyTopic;

    public MessageReplyService(KafkaTemplate<String, MessageResult> kafkaTemplate) {
        this.kafkaTemplate = kafkaTemplate;
    }

    public void send(MessageResult result) {
        kafkaTemplate.send(replyTopic, result.getCorrelationId(), result);
    }
}
