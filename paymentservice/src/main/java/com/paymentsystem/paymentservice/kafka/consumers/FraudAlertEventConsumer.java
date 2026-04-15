package com.paymentsystem.paymentservice.kafka.consumers;

import com.paymentsystem.paymentservice.domain.FraudAlert;
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
            FraudAlert alert = objectMapper.readValue(message, FraudAlert.class);
            log.info("Fraud alert received - Transaction: {} Sender: {} Risk: {}",
                    alert.getTransactionId(), alert.getSenderId(), alert.getRiskLevel());

            if(alert.getRiskLevel() == RiskLevel.HIGH) {
                transactionService.reverseTransaction(alert.getTransactionId());
                log.info("Transaction {} reversed and account frozen", alert.getTransactionId());
            }

        } catch (Exception e) {
            log.error("Failed to process fraud alert: {}", e.getMessage());
        }
    }
}