package com.hometask.server.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageJsonTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void correlationIdIsOmittedFromJsonWhenNull() throws Exception {
        Message message = new Message(1, "hello");

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).doesNotContain("correlationId");
        assertThat(json).contains("\"id\":1").contains("\"msg\":\"hello\"");
    }

    @Test
    void correlationIdIsIncludedWhenSet() throws Exception {
        Message message = new Message(1, "hello");
        message.setCorrelationId("corr-1");

        String json = objectMapper.writeValueAsString(message);

        assertThat(json).contains("\"correlationId\":\"corr-1\"");
    }
}
