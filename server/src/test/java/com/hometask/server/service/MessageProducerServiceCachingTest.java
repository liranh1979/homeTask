package com.hometask.server.service;

import com.hometask.server.dto.Message;
import com.hometask.server.dto.MessageResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.cache.concurrent.ConcurrentMapCacheManager;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.kafka.core.KafkaTemplate;

import java.util.Properties;
import java.util.concurrent.CompletableFuture;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Verifies the {@code @Cacheable}/{@code @CacheEvict} wiring on MessageProducerService using an
 * in-memory CacheManager standing in for Redis (proxy behavior is identical; only the backing store differs).
 */
class MessageProducerServiceCachingTest {

    @Configuration
    @EnableCaching
    static class CachingTestConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            Properties props = new Properties();
            props.setProperty("kafka.topic.create", "create-topic");
            props.setProperty("kafka.topic.update", "update-topic");
            props.setProperty("kafka.topic.delete", "delete-topic");
            props.setProperty("kafka.topic.read", "read-topic");
            props.setProperty("kafka.reply.timeout-seconds", "1");

            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setProperties(props);
            return configurer;
        }

        @Bean
        CacheManager cacheManager() {
            return new ConcurrentMapCacheManager("messages");
        }

        @Bean
        KafkaTemplate<String, Message> kafkaTemplate() {
            return mock(KafkaTemplate.class);
        }

        @Bean
        PendingReplyRegistry pendingReplyRegistry() {
            return mock(PendingReplyRegistry.class);
        }

        @Bean
        MessageProducerService messageProducerService(KafkaTemplate<String, Message> kafkaTemplate,
                                                        PendingReplyRegistry pendingReplyRegistry) {
            return new MessageProducerService(kafkaTemplate, pendingReplyRegistry);
        }
    }

    private AnnotationConfigApplicationContext context;
    private MessageProducerService service;
    private KafkaTemplate<String, Message> kafkaTemplate;
    private PendingReplyRegistry pendingReplyRegistry;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        context = new AnnotationConfigApplicationContext(CachingTestConfig.class);
        service = context.getBean(MessageProducerService.class);
        kafkaTemplate = (KafkaTemplate<String, Message>) context.getBean(KafkaTemplate.class);
        pendingReplyRegistry = context.getBean(PendingReplyRegistry.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    private static MessageResult successResult(int id, String msg) {
        MessageResult result = new MessageResult();
        result.setSuccess(true);
        result.setData(new Message(id, msg));
        return result;
    }

    @Test
    void secondReadOfTheSameIdIsServedFromCacheWithoutCallingKafka() {
        when(pendingReplyRegistry.register(anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResult(1, "hello")));

        service.sendRead(new Message(1, null));
        service.sendRead(new Message(1, null));

        verify(pendingReplyRegistry, times(1)).register(anyString());
    }

    @Test
    void failedReadIsNeverCached() {
        MessageResult notFound = new MessageResult();
        notFound.setSuccess(false);
        notFound.setErrorCode("NOT_FOUND");
        when(pendingReplyRegistry.register(anyString()))
                .thenReturn(CompletableFuture.completedFuture(notFound));

        service.sendRead(new Message(1, null));
        service.sendRead(new Message(1, null));

        verify(pendingReplyRegistry, times(2)).register(anyString());
    }

    @Test
    void updateEvictsTheCachedReadForThatId() {
        when(pendingReplyRegistry.register(anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResult(1, "hello")))
                .thenReturn(CompletableFuture.completedFuture(successResult(1, "hello-updated")))
                .thenReturn(CompletableFuture.completedFuture(successResult(1, "hello-updated")));

        service.sendRead(new Message(1, null));
        // sendUpdate always goes to Kafka itself; the assertion below targets the *following* read,
        // which must hit Kafka again only because the update evicted the cache entry.
        service.sendUpdate(new Message(1, "hello-updated"));
        service.sendRead(new Message(1, null));

        verify(pendingReplyRegistry, times(3)).register(anyString());
    }

    @Test
    void deleteEvictsTheCachedReadForThatId() {
        when(pendingReplyRegistry.register(anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResult(1, "hello")));

        service.sendRead(new Message(1, null));
        service.sendDelete(new Message(1, null));

        assertThat(context.getBean(CacheManager.class).getCache("messages").get(1)).isNull();
    }

    @Test
    void readingDifferentIdsAreCachedIndependently() {
        when(pendingReplyRegistry.register(anyString()))
                .thenReturn(CompletableFuture.completedFuture(successResult(1, "one")))
                .thenReturn(CompletableFuture.completedFuture(successResult(2, "two")));

        service.sendRead(new Message(1, null));
        service.sendRead(new Message(2, null));
        service.sendRead(new Message(1, null));
        service.sendRead(new Message(2, null));

        verify(pendingReplyRegistry, times(2)).register(anyString());
    }
}
