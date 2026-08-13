package com.hometask.server.service;

import com.hometask.server.dto.Message;
import com.hometask.server.dto.MessageResult;
import com.hometask.server.exception.KafkaReplyTimeoutException;
import com.hometask.server.exception.MessageProcessingException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageProducerServiceTest {

    @Mock
    private KafkaTemplate<String, Message> kafkaTemplate;

    @Mock
    private PendingReplyRegistry pendingReplyRegistry;

    private MessageProducerService service;

    @BeforeEach
    void setUp() {
        service = new MessageProducerService(kafkaTemplate, pendingReplyRegistry);
        ReflectionTestUtils.setField(service, "createTopic", "create-topic");
        ReflectionTestUtils.setField(service, "updateTopic", "update-topic");
        ReflectionTestUtils.setField(service, "deleteTopic", "delete-topic");
        ReflectionTestUtils.setField(service, "readTopic", "read-topic");
        ReflectionTestUtils.setField(service, "replyTimeoutSeconds", 1L);
    }

    private static MessageResult successResult() {
        MessageResult result = new MessageResult();
        result.setSuccess(true);
        result.setData(new Message(1, "hello"));
        return result;
    }

    @Test
    void sendCreatePublishesToCreateTopicAndReturnsReply() {
        when(pendingReplyRegistry.register(anyString())).thenReturn(CompletableFuture.completedFuture(successResult()));

        MessageResult result = service.sendCreate(new Message(1, "hello"));

        assertThat(result.isSuccess()).isTrue();
        verify(kafkaTemplate).send(eq("create-topic"), eq("1"), any(Message.class));
        verify(pendingReplyRegistry).remove(anyString());
    }

    @Test
    void sendUpdatePublishesToUpdateTopic() {
        when(pendingReplyRegistry.register(anyString())).thenReturn(CompletableFuture.completedFuture(successResult()));

        service.sendUpdate(new Message(2, "updated"));

        verify(kafkaTemplate).send(eq("update-topic"), eq("2"), any(Message.class));
    }

    @Test
    void sendDeletePublishesToDeleteTopic() {
        when(pendingReplyRegistry.register(anyString())).thenReturn(CompletableFuture.completedFuture(successResult()));

        service.sendDelete(new Message(3, null));

        verify(kafkaTemplate).send(eq("delete-topic"), eq("3"), any(Message.class));
    }

    @Test
    void sendReadPublishesToReadTopic() {
        when(pendingReplyRegistry.register(anyString())).thenReturn(CompletableFuture.completedFuture(successResult()));

        service.sendRead(new Message(4, null));

        verify(kafkaTemplate).send(eq("read-topic"), eq("4"), any(Message.class));
    }

    @Test
    void sendAssignsAFreshCorrelationIdOnTheOutgoingMessage() {
        when(pendingReplyRegistry.register(anyString())).thenReturn(CompletableFuture.completedFuture(successResult()));
        Message message = new Message(1, "hello");

        service.sendCreate(message);

        assertThat(message.getCorrelationId()).isNotBlank();
        verify(pendingReplyRegistry).register(message.getCorrelationId());
    }

    @Test
    void timesOutWhenNoReplyArrivesInTime() {
        when(pendingReplyRegistry.register(anyString())).thenReturn(new CompletableFuture<>());

        assertThatThrownBy(() -> service.sendCreate(new Message(1, "hello")))
                .isInstanceOf(KafkaReplyTimeoutException.class)
                .hasMessageContaining("id=1");
        verify(pendingReplyRegistry).remove(anyString());
    }

    @Test
    void wrapsAFailedFutureAsMessageProcessingException() {
        CompletableFuture<MessageResult> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("boom"));
        when(pendingReplyRegistry.register(anyString())).thenReturn(failed);

        assertThatThrownBy(() -> service.sendCreate(new Message(1, "hello")))
                .isInstanceOf(MessageProcessingException.class);
        verify(pendingReplyRegistry).remove(anyString());
    }

    @Test
    void restoresInterruptFlagAndWrapsAsMessageProcessingExceptionWhenInterrupted() throws InterruptedException {
        when(pendingReplyRegistry.register(anyString())).thenReturn(new CompletableFuture<>());
        AtomicReference<Throwable> caught = new AtomicReference<>();
        AtomicReference<Boolean> interruptedFlagAfterCatch = new AtomicReference<>();
        CountDownLatch started = new CountDownLatch(1);

        Thread worker = new Thread(() -> {
            started.countDown();
            try {
                service.sendCreate(new Message(1, "hello"));
            } catch (Throwable t) {
                caught.set(t);
                interruptedFlagAfterCatch.set(Thread.currentThread().isInterrupted());
            }
        });

        worker.start();
        started.await();
        Thread.sleep(100);
        worker.interrupt();
        worker.join(TimeUnit.SECONDS.toMillis(5));

        assertThat(caught.get()).isInstanceOf(MessageProcessingException.class);
        assertThat(interruptedFlagAfterCatch.get()).isTrue();
        verify(pendingReplyRegistry, times(1)).remove(anyString());
    }
}
