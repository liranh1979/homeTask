package com.hometask.server.controller;

import com.hometask.server.dto.Message;
import com.hometask.server.dto.MessageResult;
import com.hometask.server.exception.MessageConflictException;
import com.hometask.server.exception.MessageNotFoundException;
import com.hometask.server.exception.MessageProcessingException;
import com.hometask.server.service.MessageProducerService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/messages")
public class MessageController {

    private final MessageProducerService messageProducerService;

    public MessageController(MessageProducerService messageProducerService) {
        this.messageProducerService = messageProducerService;
    }

    @PostMapping
    public ResponseEntity<Message> create(@RequestBody Message message) {
        MessageResult result = messageProducerService.sendCreate(message);
        return ResponseEntity.status(HttpStatus.CREATED).body(unwrap(result));
    }

    @GetMapping("/{id}")
    public ResponseEntity<Message> read(@PathVariable int id) {
        MessageResult result = messageProducerService.sendRead(new Message(id, null));
        return ResponseEntity.ok(unwrap(result));
    }

    @PutMapping("/{id}")
    public ResponseEntity<Message> update(@PathVariable int id, @RequestBody Message message) {
        message.setId(id);
        MessageResult result = messageProducerService.sendUpdate(message);
        return ResponseEntity.ok(unwrap(result));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable int id) {
        MessageResult result = messageProducerService.sendDelete(new Message(id, null));
        unwrap(result);
        return ResponseEntity.noContent().build();
    }

    private Message unwrap(MessageResult result) {
        if (!result.isSuccess()) {
            String errorCode = result.getErrorCode();
            if ("NOT_FOUND".equals(errorCode)) {
                throw new MessageNotFoundException(result.getErrorMessage());
            }
            if ("ALREADY_EXISTS".equals(errorCode)) {
                throw new MessageConflictException(result.getErrorMessage());
            }
            throw new MessageProcessingException(result.getErrorMessage());
        }
        Message data = result.getData();
        if (data != null) {
            data.setCorrelationId(null);
        }
        return data;
    }
}
