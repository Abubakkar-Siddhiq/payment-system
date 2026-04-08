package com.paymentsystem.frauddetectionsystem.kafka;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;

import com.paymentsystem.frauddetectionsystem.services.FraudRuleEngine;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;

import java.util.List;


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;
    private final FraudRuleEngine engine;

    @KafkaListener(topics = "payment-topic", groupId = "fraud-group")
    public void consume(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            List<FraudAlert> alerts = engine.analyze(event);

            for(FraudAlert alert: alerts) {
                log.info(
                        "Transaction ID: {} - SenderID : {} - Reason : {} - Risk: {} - Detected at: {}",
                        alert.getTransactionId(),
                        alert.getSenderId(),
                        alert.getReason(),
                        alert.getRiskLevel(),
                        alert.getDetectedAt()
                );
            }

        } catch (Exception e) {
            log.error("Failed to deserialize payment event: {}", e.getMessage());
        }
    }
}