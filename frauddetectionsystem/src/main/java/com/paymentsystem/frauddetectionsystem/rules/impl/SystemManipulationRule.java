package com.paymentsystem.frauddetectionsystem.rules.impl;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.domain.enums.RiskLevel;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.rules.FraudRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Optional;

@Component
public class SystemManipulationRule implements FraudRule {

    @Override
    public Optional<FraudAlert> apply(TransactionEvent event) {
        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
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
