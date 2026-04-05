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
public class DepositRequestDto {
    @NotNull(message = "Amount is required")
    @Positive
    private BigDecimal amount;
}
