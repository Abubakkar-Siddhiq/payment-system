package com.paymentsystem.authservice.domain.dtos.request;

import com.paymentsystem.authservice.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {
    private String name;
    private String email;
    private String password;
    private Currency currency;
}

