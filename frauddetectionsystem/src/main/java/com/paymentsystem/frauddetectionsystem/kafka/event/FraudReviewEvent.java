package com.paymentsystem.frauddetectionsystem.kafka.event;

import com.paymentsystem.frauddetectionsystem.domain.enums.FraudStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FraudReviewEvent {
    private UUID fraudAlertId;
    private UUID transactionId;
    private FraudStatus status;
}