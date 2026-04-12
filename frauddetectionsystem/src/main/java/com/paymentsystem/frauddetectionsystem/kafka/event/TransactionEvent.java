package com.paymentsystem.frauddetectionsystem.kafka.event;

import com.paymentsystem.frauddetectionsystem.domain.enums.Currency;
import com.paymentsystem.frauddetectionsystem.domain.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionEvent {
    private UUID transactionId;
    private UUID senderId;
    private UUID receiverId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime timestamp;
    private Currency currency;
}
