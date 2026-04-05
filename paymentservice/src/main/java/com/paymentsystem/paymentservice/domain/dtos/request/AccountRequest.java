package com.paymentsystem.paymentservice.domain.dtos.request;

import lombok.*;

import com.paymentsystem.paymentservice.domain.enums.Currency;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class AccountRequest {
//    private UUID id;
    private String owner;
//    private BigDecimal amount;
    private Currency currency;
//    private LocalDateTime createdAt;
}
