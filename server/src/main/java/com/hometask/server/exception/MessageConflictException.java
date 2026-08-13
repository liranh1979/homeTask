package com.hometask.server.exception;

public class MessageConflictException extends RuntimeException {

    public MessageConflictException(String message) {
        super(message);
    }
}
