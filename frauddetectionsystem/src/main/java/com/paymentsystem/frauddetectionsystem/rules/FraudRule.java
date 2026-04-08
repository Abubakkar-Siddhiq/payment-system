package com.paymentsystem.frauddetectionsystem.rules;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.kafka.event.TransactionEvent;

import java.util.Optional;

public interface FraudRule {
    Optional<FraudAlert> apply(TransactionEvent event);
}
