package com.paymentsystem.frauddetectionsystem.services;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.domain.enums.FraudStatus;
import com.paymentsystem.frauddetectionsystem.kafka.event.FraudReviewEvent;
import com.paymentsystem.frauddetectionsystem.outbox.OutboxEvent;
import com.paymentsystem.frauddetectionsystem.repositories.FraudAlertRepository;
import com.paymentsystem.frauddetectionsystem.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.UUID;

@RequiredArgsConstructor
public class FraudService {

    private final FraudAlertRepository fraudAlertRepository;
    private final OutboxEventRepository outboxEventRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public FraudAlert approve(UUID id, String reviewedBy) {
        return review(id, FraudStatus.APPROVED, reviewedBy);
    }

    @Transactional
    public FraudAlert reject(UUID id, String reviewedBy) {
        return review(id, FraudStatus.REJECTED, reviewedBy);
    }

    public FraudAlert review(UUID id, FraudStatus status, String reviewedBy) {
        FraudAlert alert = fraudAlertRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Alert not found"));

        alert.setStatus(status);
        alert.setReviewedBy(UUID.fromString(reviewedBy));
        alert.setReviewedAt(LocalDateTime.now());
        FraudAlert saved = fraudAlertRepository.save(alert);

        FraudReviewEvent event =FraudReviewEvent.builder()
                .fraudAlertId(alert.getId())
                .transactionId(alert.getTransactionId())
                .status(alert.getStatus())
                .build();

        try {
            String eventMessage = objectMapper.writeValueAsString(event);

            OutboxEvent outboxEvent = new OutboxEvent();
            outboxEvent.setPayload(eventMessage);

            outboxEventRepository.save(outboxEvent);

        } catch (Exception e) {
            throw new RuntimeException("Failed to serialize event", e);
        }

        return saved;
    }

}
