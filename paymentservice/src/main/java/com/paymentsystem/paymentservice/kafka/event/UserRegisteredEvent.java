package com.paymentsystem.paymentservice.kafka.event;

import com.paymentsystem.paymentservice.domain.enums.Currency;
import lombok.Builder;
import lombok.Data;

import java.util.UUID;

@Data
@Builder
public class UserRegisteredEvent {
    private UUID userId;
    private String name;
    private String email;
    private Currency currency;
}
