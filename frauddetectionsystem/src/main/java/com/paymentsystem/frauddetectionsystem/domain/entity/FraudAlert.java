package com.paymentsystem.frauddetectionsystem.domain.entity;

import com.paymentsystem.frauddetectionsystem.domain.enums.RiskLevel;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class FraudAlert {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    private UUID transactionId;
    private UUID senderId;
    private BigDecimal amount;
    private String reason;

    @Enumerated(EnumType.STRING)
    private RiskLevel riskLevel; // LOW, MEDIUM, HIGH

    private LocalDateTime detectedAt;
}
