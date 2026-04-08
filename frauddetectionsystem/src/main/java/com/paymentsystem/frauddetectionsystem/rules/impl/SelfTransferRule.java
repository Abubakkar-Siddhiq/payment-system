package com.paymentsystem.frauddetectionsystem.rules.impl;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.domain.enums.RiskLevel;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.rules.FraudRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class SelfTransferRule implements FraudRule {
    private static final BigDecimal LIMIT = new BigDecimal("10000");

    @Override
    public Optional<FraudAlert> apply(TransactionEvent event) {
        if (event.getSenderId().equals(event.getReceiverId())) {
            return Optional.of(FraudAlert.builder()
                    .senderId(event.getSenderId())
                    .transactionId(event.getTransactionId())
                    .amount(event.getAmount())
                    .reason("Suspicious Large Transaction")
                    .riskLevel(RiskLevel.HIGH)
                    .detectedAt(event.getTimestamp())
                    .build());
        }
        return Optional.empty();
    }
}
