package com.hometask.producer.listener;

import com.hometask.producer.dto.Message;
import com.hometask.producer.dto.MessageResult;
import com.hometask.producer.entity.MessageEntity;
import com.hometask.producer.exception.MessageAlreadyExistsException;
import com.hometask.producer.exception.MessageNotFoundException;
import com.hometask.producer.service.MessageReplyService;
import com.hometask.producer.service.MessageService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MessageEventListenerTest {

    @Mock
    private MessageService messageService;

    @Mock
    private MessageReplyService messageReplyService;

    private MessageEventListener listener;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        listener = new MessageEventListener(messageService, messageReplyService);
    }

    private MessageResult captureReply() {
        ArgumentCaptor<MessageResult> captor = ArgumentCaptor.forClass(MessageResult.class);
        verify(messageReplyService).send(captor.capture());
        return captor.getValue();
    }

    private static Message message(int id, String msg, String correlationId) {
        Message m = new Message(id, msg);
        m.setCorrelationId(correlationId);
        return m;
    }

    @Test
    void onCreateSuccessSendsSuccessReplyEchoingTheMessage() {
        Message request = message(1, "hello", "corr-1");

        listener.onCreate(request);

        verify(messageService).create(1, "hello");
        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isTrue();
        assertThat(reply.getCorrelationId()).isEqualTo("corr-1");
        assertThat(reply.getData()).isSameAs(request);
    }

    @Test
    void onCreateAlreadyExistsSendsAlreadyExistsFailure() {
        doThrow(new MessageAlreadyExistsException("Message already exists with id=1"))
                .when(messageService).create(1, "hello");

        listener.onCreate(message(1, "hello", "corr-1"));

        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isFalse();
        assertThat(reply.getErrorCode()).isEqualTo("ALREADY_EXISTS");
        assertThat(reply.getErrorMessage()).isEqualTo("Message already exists with id=1");
    }

    @Test
    void onCreateUnexpectedFailureSendsInternalErrorFailure() {
        doThrow(new RuntimeException("db down")).when(messageService).create(1, "hello");

        listener.onCreate(message(1, "hello", "corr-1"));

        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isFalse();
        assertThat(reply.getErrorCode()).isEqualTo("INTERNAL_ERROR");
        assertThat(reply.getErrorMessage()).isEqualTo("db down");
    }

    @Test
    void onUpdateSuccessSendsSuccessReply() {
        listener.onUpdate(message(1, "updated", "corr-2"));

        verify(messageService).update(1, "updated");
        assertThat(captureReply().isSuccess()).isTrue();
    }

    @Test
    void onUpdateNotFoundSendsNotFoundFailure() {
        doThrow(new MessageNotFoundException("Message not found with id=99"))
                .when(messageService).update(99, "updated");

        listener.onUpdate(message(99, "updated", "corr-2"));

        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isFalse();
        assertThat(reply.getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void onDeleteSuccessSendsSuccessReplyWithNullData() {
        listener.onDelete(message(1, null, "corr-3"));

        verify(messageService).delete(1);
        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isTrue();
        assertThat(reply.getData()).isNull();
    }

    @Test
    void onDeleteNotFoundSendsNotFoundFailure() {
        doThrow(new MessageNotFoundException("Message not found with id=99"))
                .when(messageService).delete(99);

        listener.onDelete(message(99, null, "corr-3"));

        assertThat(captureReply().getErrorCode()).isEqualTo("NOT_FOUND");
    }

    @Test
    void onReadSuccessSendsSuccessReplyWithEntityData() {
        when(messageService.read(1)).thenReturn(new MessageEntity(1, "hello"));

        listener.onRead(message(1, null, "corr-4"));

        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isTrue();
        assertThat(reply.getData().getId()).isEqualTo(1);
        assertThat(reply.getData().getMsg()).isEqualTo("hello");
    }

    @Test
    void onReadNotFoundSendsNotFoundFailure() {
        doThrow(new MessageNotFoundException("Message not found with id=99"))
                .when(messageService).read(99);

        listener.onRead(message(99, null, "corr-4"));

        MessageResult reply = captureReply();
        assertThat(reply.isSuccess()).isFalse();
        assertThat(reply.getErrorCode()).isEqualTo("NOT_FOUND");
    }
}
