package com.paymentsystem.authservice.kafka.event;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserRegisterdProducer {
    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishEvent(String message) {
        log.info("Publishing User Registered event: {}", message);
        kafkaTemplate.send("user-registered-topic", message);
    }
}
