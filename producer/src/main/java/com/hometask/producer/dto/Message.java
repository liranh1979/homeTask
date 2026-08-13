package com.hometask.producer.dto;

import com.fasterxml.jackson.annotation.JsonCreator;

public class Message {

    private int id;
    private String msg;
    private String correlationId;

    @JsonCreator
    public Message() {
    }

    public Message(int id, String msg) {
        this.id = id;
        this.msg = msg;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public String getCorrelationId() {
        return correlationId;
    }

    public void setCorrelationId(String correlationId) {
        this.correlationId = correlationId;
    }

    @Override
    public String toString() {
        return "Message{id=" + id + ", msg='" + msg + "', correlationId='" + correlationId + "'}";
    }
}
