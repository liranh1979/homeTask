package com.hometask.producer.service;

import com.hometask.producer.dto.MessageResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MessageReplyServiceTest {

    @Mock
    private KafkaTemplate<String, MessageResult> kafkaTemplate;

    private MessageReplyService messageReplyService;

    @BeforeEach
    void setUp() {
        messageReplyService = new MessageReplyService(kafkaTemplate);
        ReflectionTestUtils.setField(messageReplyService, "replyTopic", "reply-topic");
    }

    @Test
    void sendPublishesToTheReplyTopicKeyedByCorrelationId() {
        MessageResult result = MessageResult.success("corr-1", null);

        messageReplyService.send(result);

        verify(kafkaTemplate).send("reply-topic", "corr-1", result);
    }
}
