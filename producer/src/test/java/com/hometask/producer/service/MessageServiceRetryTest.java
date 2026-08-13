package com.hometask.producer.service;

import com.hometask.producer.exception.MessageAlreadyExistsException;
import com.hometask.producer.repository.MessageRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.support.PropertySourcesPlaceholderConfigurer;
import org.springframework.retry.annotation.EnableRetry;

import java.util.Properties;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Proves the declarative {@code @Retryable} policy on MessageService actually retries transient
 * failures up to the configured max attempts, and does NOT retry business-rule exceptions.
 */
class MessageServiceRetryTest {

    @Configuration
    @EnableRetry
    static class RetryTestConfig {

        @Bean
        static PropertySourcesPlaceholderConfigurer propertySourcesPlaceholderConfigurer() {
            Properties props = new Properties();
            props.setProperty("app.retry.max-attempts", "3");
            props.setProperty("app.retry.delay-ms", "5");

            PropertySourcesPlaceholderConfigurer configurer = new PropertySourcesPlaceholderConfigurer();
            configurer.setProperties(props);
            return configurer;
        }

        @Bean
        MessageRepository messageRepository() {
            return mock(MessageRepository.class);
        }

        @Bean
        MessageService messageService(MessageRepository messageRepository) {
            return new MessageService(messageRepository);
        }
    }

    private AnnotationConfigApplicationContext context;
    private MessageService messageService;
    private MessageRepository messageRepository;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(RetryTestConfig.class);
        messageService = context.getBean(MessageService.class);
        messageRepository = context.getBean(MessageRepository.class);
    }

    @AfterEach
    void tearDown() {
        context.close();
    }

    @Test
    void retriesTransientFailuresAndSucceedsBeforeExhaustingMaxAttempts() {
        when(messageRepository.existsById(1))
                .thenThrow(new RuntimeException("db unavailable"))
                .thenThrow(new RuntimeException("db unavailable"))
                .thenReturn(false);

        messageService.create(1, "hello");

        verify(messageRepository, times(3)).existsById(1);
        verify(messageRepository, times(1)).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void givesUpAfterConfiguredMaxAttemptsAndRethrowsTheLastFailure() {
        when(messageRepository.existsById(1)).thenThrow(new RuntimeException("db unavailable"));

        assertThatThrownBy(() -> messageService.create(1, "hello"))
                .isInstanceOf(RuntimeException.class)
                .hasMessage("db unavailable");

        verify(messageRepository, times(3)).existsById(1);
        verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void doesNotRetryBusinessRuleExceptions() {
        when(messageRepository.existsById(1)).thenReturn(true);

        assertThatThrownBy(() -> messageService.create(1, "hello"))
                .isInstanceOf(MessageAlreadyExistsException.class);

        verify(messageRepository, times(1)).existsById(eq(1));
    }
}
