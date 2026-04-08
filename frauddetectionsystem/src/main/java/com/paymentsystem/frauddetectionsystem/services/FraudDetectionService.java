package com.paymentsystem.frauddetectionsystem.services;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.domain.enums.RiskLevel;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.repositories.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FraudDetectionService {

    private final FraudAlertRepository fraudAlertRepository;

    public Optional<FraudAlert> analyze(TransactionEvent event) {

        if (event.getAmount().compareTo(new BigDecimal("10000")) > 0) {
            return createAndSave(event, "Suspicious Large Transaction", RiskLevel.HIGH);
        }

        if (event.getAmount().remainder(BigDecimal.ONE).compareTo(BigDecimal.ZERO) == 0) {
            return createAndSave(event, "Round numbers", RiskLevel.LOW);
        }

        if (event.getSenderId().equals(event.getReceiverId())) {
            return createAndSave(event, "Self Transfer", RiskLevel.MEDIUM);
        }

        if (event.getAmount().compareTo(BigDecimal.ZERO) <= 0) {
            return createAndSave(event, "System manipulation Attempt", RiskLevel.HIGH);
        }

        if (event.getAmount().compareTo(new BigDecimal("9999")) == 0) {
            return createAndSave(event, "Threshold Gaming", RiskLevel.HIGH);
        }

        return Optional.empty();
    }

    private Optional<FraudAlert> createAndSave(TransactionEvent event, String reason, RiskLevel riskLevel) {

        FraudAlert alert = FraudAlert.builder()
                .senderId(event.getSenderId())
                .transactionId(event.getTransactionId())
                .amount(event.getAmount())
                .reason(reason)
                .riskLevel(riskLevel)
                .detectedAt(event.getTimestamp())
                .build();

        return Optional.of(fraudAlertRepository.save(alert));
    }
}
