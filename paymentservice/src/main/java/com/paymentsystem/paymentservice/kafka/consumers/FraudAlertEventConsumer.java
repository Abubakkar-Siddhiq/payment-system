package com.paymentsystem.paymentservice.kafka.consumers;

import com.paymentsystem.paymentservice.kafka.event.FraudAlertEvent;
import com.paymentsystem.paymentservice.domain.enums.RiskLevel;

import com.paymentsystem.paymentservice.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
@Slf4j
public class FraudAlertEventConsumer {

    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

    @KafkaListener(topics = "fraud-alert-topic")
    public void consume(String message) {
        log.info("FRAUD ALERT: {}", message);
        try {
            FraudAlertEvent alert = objectMapper.readValue(message, FraudAlertEvent.class);
            log.info("Fraud alert received - Transaction: {} Sender: {} Risk: {}",
                    alert.getTransactionId(), alert.getSenderId(), alert.getRiskLevel());

            if(alert.getRiskLevel() == RiskLevel.HIGH) {
                transactionService.freezeAccount(alert.getTransactionId());
                // Reversal is done upon review by the Admin. A human in the loop for now. Real world systems use AI.
                log.info("Transaction {} - Sender's account Frozen", alert.getTransactionId());
            }

        } catch (Exception e) {
            log.error("Failed to process fraud alert: {}", e.getMessage());
        }
    }
}