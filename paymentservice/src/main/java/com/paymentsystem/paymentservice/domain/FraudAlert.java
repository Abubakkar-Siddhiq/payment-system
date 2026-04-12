package com.paymentsystem.paymentservice.domain;

import com.paymentsystem.paymentservice.domain.enums.RiskLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FraudAlert {
    private UUID id;
    private UUID transactionId;
    private UUID senderId;
    private BigDecimal amount;
    private String reason;
    private RiskLevel riskLevel;
    private LocalDateTime detectedAt;
}