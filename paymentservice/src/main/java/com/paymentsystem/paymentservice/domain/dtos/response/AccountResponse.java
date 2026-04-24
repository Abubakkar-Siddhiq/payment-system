package com.paymentsystem.paymentservice.domain.dtos.response;

import com.paymentsystem.paymentservice.domain.enums.Currency;
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
public class AccountResponse {
    private UUID id;
    private String owner;
    private String number;
    private BigDecimal balance;
    private Currency currency;
    private LocalDateTime createdAt;
}
