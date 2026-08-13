package com.hometask.server.controller;

import com.hometask.server.dto.Message;
import com.hometask.server.dto.MessageResult;
import com.hometask.server.exception.KafkaReplyTimeoutException;
import com.hometask.server.exception.MessageProcessingException;
import com.hometask.server.service.MessageProducerService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.cache.CacheManager;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
class MessageControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MessageProducerService messageProducerService;

    @MockitoBean
    private CacheManager cacheManager;

    private static MessageResult success(Message data) {
        MessageResult result = new MessageResult();
        result.setSuccess(true);
        result.setData(data);
        return result;
    }

    private static MessageResult failure(String errorCode, String errorMessage) {
        MessageResult result = new MessageResult();
        result.setSuccess(false);
        result.setErrorCode(errorCode);
        result.setErrorMessage(errorMessage);
        return result;
    }

    @Test
    void createReturns201WithBody() throws Exception {
        when(messageProducerService.sendCreate(any(Message.class))).thenReturn(success(new Message(1, "hello")));

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"msg\":\"hello\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.msg").value("hello"))
                .andExpect(jsonPath("$.correlationId").doesNotExist());
    }

    @Test
    void createOfAnExistingIdReturns409() throws Exception {
        when(messageProducerService.sendCreate(any(Message.class)))
                .thenReturn(failure("ALREADY_EXISTS", "Message already exists with id=1"));

        mockMvc.perform(post("/messages")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"id\":1,\"msg\":\"hello\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.error").value("Message already exists with id=1"));
    }

    @Test
    void readReturns200WithBody() throws Exception {
        when(messageProducerService.sendRead(any(Message.class))).thenReturn(success(new Message(1, "hello")));

        mockMvc.perform(get("/messages/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.msg").value("hello"));
    }

    @Test
    void readOfMissingIdReturns404() throws Exception {
        when(messageProducerService.sendRead(any(Message.class)))
                .thenReturn(failure("NOT_FOUND", "Message not found with id=99"));

        mockMvc.perform(get("/messages/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.error").value("Message not found with id=99"));
    }

    @Test
    void updateReturns200WithBody() throws Exception {
        when(messageProducerService.sendUpdate(any(Message.class))).thenReturn(success(new Message(1, "updated")));

        mockMvc.perform(put("/messages/1")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"msg\":\"updated\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.msg").value("updated"));
    }

    @Test
    void updateOfMissingIdReturns404() throws Exception {
        when(messageProducerService.sendUpdate(any(Message.class)))
                .thenReturn(failure("NOT_FOUND", "Message not found with id=99"));

        mockMvc.perform(put("/messages/99")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"msg\":\"updated\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void deleteReturns204() throws Exception {
        when(messageProducerService.sendDelete(any(Message.class))).thenReturn(success(null));

        mockMvc.perform(delete("/messages/1"))
                .andExpect(status().isNoContent())
                .andExpect(content().string(""));
    }

    @Test
    void deleteOfMissingIdReturns404() throws Exception {
        when(messageProducerService.sendDelete(any(Message.class)))
                .thenReturn(failure("NOT_FOUND", "Message not found with id=99"));

        mockMvc.perform(delete("/messages/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void timeoutWaitingForKafkaReplyReturns504() throws Exception {
        when(messageProducerService.sendRead(any(Message.class)))
                .thenThrow(new KafkaReplyTimeoutException("Timed out waiting for confirmation of message id=1"));

        mockMvc.perform(get("/messages/1"))
                .andExpect(status().isGatewayTimeout())
                .andExpect(jsonPath("$.error").value("Timed out waiting for confirmation of message id=1"));
    }

    @Test
    void unexpectedProcessingFailureReturns500() throws Exception {
        when(messageProducerService.sendRead(any(Message.class)))
                .thenThrow(new MessageProcessingException("Failed to process message id=1"));

        mockMvc.perform(get("/messages/1"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.error").value("Failed to process message id=1"));
    }
}
