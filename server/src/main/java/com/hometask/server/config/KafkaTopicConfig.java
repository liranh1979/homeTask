package com.hometask.server.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
public class KafkaTopicConfig {

    @Value("${kafka.topic.create}")
    private String createTopic;

    @Value("${kafka.topic.update}")
    private String updateTopic;

    @Value("${kafka.topic.delete}")
    private String deleteTopic;

    @Value("${kafka.topic.read}")
    private String readTopic;

    @Value("${kafka.topic.reply}")
    private String replyTopic;

    @Bean
    public NewTopic createTopic() {
        return TopicBuilder.name(createTopic).build();
    }

    @Bean
    public NewTopic updateTopic() {
        return TopicBuilder.name(updateTopic).build();
    }

    @Bean
    public NewTopic deleteTopic() {
        return TopicBuilder.name(deleteTopic).build();
    }

    @Bean
    public NewTopic readTopic() {
        return TopicBuilder.name(readTopic).build();
    }

    @Bean
    public NewTopic replyTopic() {
        return TopicBuilder.name(replyTopic).build();
    }
}
