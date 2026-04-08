package com.paymentsystem.frauddetectionsystem.services;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.kafka.FraudAlertEventProducer;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;
import com.paymentsystem.frauddetectionsystem.repositories.FraudAlertRepository;
import com.paymentsystem.frauddetectionsystem.rules.FraudRule;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class FraudRuleEngine {

    private final List<FraudRule> rules;
    private final FraudAlertRepository repository;
    private final FraudAlertEventProducer eventProducer;
    private final ObjectMapper objectMapper;

    public List<FraudAlert> analyze(TransactionEvent event) {

        List<FraudAlert> alerts = new ArrayList<>();

        for (FraudRule rule : rules) {
            rule.apply(event).ifPresent(alert -> {
                FraudAlert saved = repository.save(alert);

                String alertMessage = objectMapper.writeValueAsString(alert);
                eventProducer.publishFraudAlertEvent(alertMessage);

                alerts.add(saved);
            });
        }

        return alerts;
    }
}