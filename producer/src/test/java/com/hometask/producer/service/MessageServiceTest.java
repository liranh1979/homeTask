package com.hometask.producer.service;

import com.hometask.producer.entity.MessageEntity;
import com.hometask.producer.exception.MessageAlreadyExistsException;
import com.hometask.producer.exception.MessageNotFoundException;
import com.hometask.producer.repository.MessageRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageServiceTest {

    @Mock
    private MessageRepository messageRepository;

    private MessageService messageService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        messageService = new MessageService(messageRepository);
    }

    @Test
    void createSavesWhenIdDoesNotExist() {
        when(messageRepository.existsById(1)).thenReturn(false);

        messageService.create(1, "hello");

        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getId()).isEqualTo(1);
        assertThat(captor.getValue().getMsg()).isEqualTo("hello");
    }

    @Test
    void createThrowsWhenIdAlreadyExists() {
        when(messageRepository.existsById(1)).thenReturn(true);

        assertThatThrownBy(() -> messageService.create(1, "hello"))
                .isInstanceOf(MessageAlreadyExistsException.class)
                .hasMessageContaining("id=1");
        verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void updateSavesWhenIdExists() {
        when(messageRepository.existsById(1)).thenReturn(true);

        messageService.update(1, "updated");

        ArgumentCaptor<MessageEntity> captor = ArgumentCaptor.forClass(MessageEntity.class);
        verify(messageRepository).save(captor.capture());
        assertThat(captor.getValue().getMsg()).isEqualTo("updated");
    }

    @Test
    void updateThrowsWhenIdDoesNotExist() {
        when(messageRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> messageService.update(99, "updated"))
                .isInstanceOf(MessageNotFoundException.class)
                .hasMessageContaining("id=99");
        verify(messageRepository, never()).save(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void deleteRemovesWhenIdExists() {
        when(messageRepository.existsById(1)).thenReturn(true);

        messageService.delete(1);

        verify(messageRepository).deleteById(1);
    }

    @Test
    void deleteThrowsWhenIdDoesNotExist() {
        when(messageRepository.existsById(99)).thenReturn(false);

        assertThatThrownBy(() -> messageService.delete(99))
                .isInstanceOf(MessageNotFoundException.class)
                .hasMessageContaining("id=99");
        verify(messageRepository, never()).deleteById(org.mockito.ArgumentMatchers.anyInt());
    }

    @Test
    void readReturnsEntityWhenIdExists() {
        MessageEntity entity = new MessageEntity(1, "hello");
        when(messageRepository.findById(1)).thenReturn(Optional.of(entity));

        MessageEntity result = messageService.read(1);

        assertThat(result).isSameAs(entity);
    }

    @Test
    void readThrowsWhenIdDoesNotExist() {
        when(messageRepository.findById(99)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> messageService.read(99))
                .isInstanceOf(MessageNotFoundException.class)
                .hasMessageContaining("id=99");
    }
}
