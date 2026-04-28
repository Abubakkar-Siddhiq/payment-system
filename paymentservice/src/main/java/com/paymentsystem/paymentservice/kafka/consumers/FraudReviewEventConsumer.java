package com.paymentsystem.paymentservice.kafka.consumers;

import com.paymentsystem.paymentservice.domain.enums.FraudStatus;
import com.paymentsystem.paymentservice.kafka.event.FraudReviewEvent;
import com.paymentsystem.paymentservice.services.TransactionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
@Slf4j
public class FraudReviewEventConsumer {

    private final ObjectMapper objectMapper;
    private final TransactionService transactionService;

    @KafkaListener(topics = "fraud-review-topic")
    public void consume(String message) {
        try {

            FraudReviewEvent review = objectMapper.readValue(message, FraudReviewEvent.class);
            log.info("Fraud review received - Transaction: {} Alert: {} Status: {}",
                    review.getTransactionId(), review.getFraudAlertId(), review.getStatus());

            if(review.getStatus() == FraudStatus.APPROVED) {
                transactionService.unfreezeAccount(review.getTransactionId());
            } else if (review.getStatus() == FraudStatus.REJECTED) {
                transactionService.reverseTransaction(review.getTransactionId());
                log.info("Transaction Reversed: {}", review.getTransactionId());
            }

        } catch (Exception e) {
            log.error("Failed to process fraud review: {}", e.getMessage());
            throw new RuntimeException(e);
        }
    }
}
