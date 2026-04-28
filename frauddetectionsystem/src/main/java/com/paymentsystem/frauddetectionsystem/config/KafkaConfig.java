package com.paymentsystem.frauddetectionsystem.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.TopicBuilder;

@Configuration
@EnableKafka
public class KafkaConfig {

    @Bean
    public NewTopic fraudAlertTopic() {
        return TopicBuilder.name("fraud-alert-topic").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic fraudReviewTopic() { // Approve / Reject
        return TopicBuilder.name("fraud-review-topic").partitions(1).replicas(1).build();
    }

    @Bean
    public NewTopic paymentTopic() {
        return TopicBuilder.name("payment-topic").partitions(1).replicas(1).build();
    }
}
