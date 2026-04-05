package com.paymentsystem.paymentservice.domain.dtos.request;

import com.paymentsystem.paymentservice.domain.enums.Currency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.UUID;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest {
    private UUID sender;
    private UUID receiver;
    private BigDecimal amount;
    private Currency currency;
}
