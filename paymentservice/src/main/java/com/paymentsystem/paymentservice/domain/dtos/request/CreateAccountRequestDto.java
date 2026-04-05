package com.paymentsystem.paymentservice.domain.dtos.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.paymentsystem.paymentservice.domain.enums.Currency;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class CreateAccountRequestDto {
    @NotBlank(message = "Owner name is required")
    private String owner;

    @NotNull(message = "Currency is required")
    private Currency currency;
}
