package com.hometask.producer.dto;

public class MessageResult {

    private String correlationId;
    private boolean success;
    private String errorCode;
    private String errorMessage;
    private Message data;

    public MessageResult() {
    }

    public static MessageResult success(String correlationId, Message data) {
        MessageResult result = new MessageResult();
        result.correlationId = correlationId;
        result.success = true;
        result.data = data;
        return result;
    }

    public static MessageResult failure(String correlationId, String errorCode, String errorMessage) {
        MessageResult result = new MessageResult();
        result.correlationId = correlationId;
        result.success = false;
        result.errorCode = errorCode;
        result.errorMessage = errorMessage;
        return result;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    public boolean isSuccess() {
        return success;
    }

    public void setSuccess(boolean success) {
        this.success = success;
    }

    public String getErrorCode() {
        return errorCode;
    }

    public void setErrorCode(String errorCode) {
        this.errorCode = errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    public Message getData() {
        return data;
    }

    public void setData(Message data) {
        this.data = data;
    }
}
