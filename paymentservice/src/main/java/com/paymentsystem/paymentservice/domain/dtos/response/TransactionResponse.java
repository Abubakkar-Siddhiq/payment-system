package com.paymentsystem.paymentservice.domain.dtos.response;

import com.paymentsystem.paymentservice.domain.enums.Currency;
import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
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
public class TransactionResponse {
    private UUID id;
    private UUID sender;
    private UUID receiver;
    private BigDecimal amount;
    private Currency currency;
    private TransactionStatus status;
    private LocalDateTime createdAt;
}
