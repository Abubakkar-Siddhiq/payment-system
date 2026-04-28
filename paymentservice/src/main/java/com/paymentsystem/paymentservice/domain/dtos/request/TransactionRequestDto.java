package com.paymentsystem.paymentservice.domain.dtos.request;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class TransactionRequestDto {

    @NotNull(message = "Sender account number is required")
    private String sender;

    @NotNull(message = "Receiver account number is required")
    private String receiver;

    @NotNull @Positive(message = "Amount is required")
    private BigDecimal amount;
}
