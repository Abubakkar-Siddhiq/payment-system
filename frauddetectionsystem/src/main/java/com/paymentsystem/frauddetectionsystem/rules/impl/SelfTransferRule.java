package com.paymentsystem.frauddetectionsystem.rules.impl;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.domain.enums.RiskLevel;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.rules.FraudRule;
import org.springframework.stereotype.Component;
import java.util.Optional;

@Component
public class SelfTransferRule implements FraudRule {

    @Override
    public Optional<FraudAlert> apply(TransactionEvent event) {
        if (event.getSenderId().equals(event.getReceiverId())) {
            return Optional.of(FraudAlert.builder()
                    .senderId(event.getSenderId())
                    .transactionId(event.getTransactionId())
                    .amount(event.getAmount())
                    .reason("Suspicious Large Transaction")
                    .riskLevel(RiskLevel.MEDIUM)
                    .detectedAt(event.getTimestamp())
                    .build());
        }
        return Optional.empty();
    }
}
