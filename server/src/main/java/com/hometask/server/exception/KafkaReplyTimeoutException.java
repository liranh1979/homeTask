package com.hometask.server.exception;

public class KafkaReplyTimeoutException extends RuntimeException {

    public KafkaReplyTimeoutException(String message) {
        super(message);
    }
}
