package com.paymentsystem.paymentservice.domain.dtos.request;

import com.paymentsystem.paymentservice.domain.enums.Currency;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequest {
    private String sender; // Sender Account number
    private String receiver; // Receiver Account number
    private BigDecimal amount;
    private Currency currency;
}
