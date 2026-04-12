package com.paymentsystem.frauddetectionsystem.rules.impl;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.domain.enums.RiskLevel;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.rules.FraudRule;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.Map;
import java.util.Optional;

@Component
public class LargeTransactionRule implements FraudRule {
    private static final Map<String, BigDecimal> LIMITS = Map.of(
            "USD", new BigDecimal("10000"),
            "EUR", new BigDecimal("9000"),
            "GBP", new BigDecimal("8000"),
            "JPY", new BigDecimal("1000000"),
            "CAD", new BigDecimal("13000"),
            "AUD", new BigDecimal("14000"),
            "CHF", new BigDecimal("9500")
    );

    public BigDecimal getThreshold(String currency) {
        return LIMITS.getOrDefault(currency, BigDecimal.ZERO);
    }

    @Override
    public Optional<FraudAlert> apply(TransactionEvent event) {

        BigDecimal LIMIT = getThreshold(String.valueOf(event.getCurrency()));

        if (event.getAmount().compareTo(LIMIT) >= 0) {
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
