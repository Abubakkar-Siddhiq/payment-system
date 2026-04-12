package com.paymentsystem.frauddetectionsystem.kafka;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudAlertEventProducer {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void publishFraudAlertEvent(String message) {
        log.info("Publishing payment event: {}", message);
        kafkaTemplate.send("fraud-alert-topic", message);
    }
}

