package com.hometask.producer.exception;

public class MessageAlreadyExistsException extends RuntimeException {

    public MessageAlreadyExistsException(String message) {
        super(message);
    }
}
