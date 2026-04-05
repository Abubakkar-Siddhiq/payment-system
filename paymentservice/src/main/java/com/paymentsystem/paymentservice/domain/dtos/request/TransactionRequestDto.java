package com.paymentsystem.paymentservice.domain.dtos.request;

import com.paymentsystem.paymentservice.domain.enums.Currency;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
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
public class TransactionRequestDto {

    @NotNull(message = "Sender is required")
    private UUID sender;

    @NotNull(message = "Receiver is required")
    private UUID receiver;

    @NotNull @Positive(message = "Amount is required")
    private BigDecimal amount;

    @NotNull(message = "Currency is required")
    private Currency currency;
}
