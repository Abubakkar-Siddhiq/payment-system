package com.paymentsystem.frauddetectionsystem.services;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.kafka.FraudAlertEventProducer;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.repositories.FraudAlertRepository;
import com.paymentsystem.frauddetectionsystem.rules.FraudRule;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class FraudRuleEngine {

    private final List<FraudRule> rules;
    private final FraudAlertRepository repository;
    private final FraudAlertEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public List<FraudAlert> analyze(TransactionEvent event) {

        List<FraudAlert> alerts = new ArrayList<>();

        for (FraudRule rule : rules) {
            Optional<FraudAlert> alert = rule.apply(event);
            if(alert.isPresent()) {
                FraudAlert saved = repository.save(alert.get());
                try {
                    String alertMessage = objectMapper.writeValueAsString(saved);
                    eventProducer.publishFraudAlertEvent(alertMessage);
                } catch (Exception e) {
                    log.error("Failed to serialize fraud alert: {}", e.getMessage());
                }
                alerts.add(saved);
                break;
            }
        }

        return alerts;
    }
}