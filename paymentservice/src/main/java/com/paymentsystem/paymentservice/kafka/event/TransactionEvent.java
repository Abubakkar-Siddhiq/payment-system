package com.paymentsystem.paymentservice.kafka.event;

import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
import lombok.Builder;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

//@Setter
@Builder
public class TransactionEvent {
    private UUID transactionId;
    private UUID senderId;
    private UUID receiverId;
    private BigDecimal amount;
    private TransactionStatus status;
    private LocalDateTime timestamp;
}
