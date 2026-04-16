package com.paymentsystem.paymentservice.kafka.event;

import com.paymentsystem.paymentservice.domain.enums.Currency;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserRegisteredEvent {
    private UUID id;
    private String name;
    private String email;
    private Currency currency;
}
