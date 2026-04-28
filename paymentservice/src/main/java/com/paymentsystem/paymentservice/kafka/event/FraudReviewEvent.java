package com.paymentsystem.paymentservice.kafka.event;

import com.paymentsystem.paymentservice.domain.enums.FraudStatus;
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