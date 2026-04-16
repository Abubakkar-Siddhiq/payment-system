package com.paymentsystem.paymentservice.kafka.consumers;

import com.paymentsystem.paymentservice.kafka.event.UserRegisteredEvent;
import com.paymentsystem.paymentservice.services.AccountService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
@Slf4j
public class UserRegisteredEventConsumer {

    private final ObjectMapper objectMapper;
    private final AccountService accountService;

    @KafkaListener(topics = "user-registered-topic")
    public void consume(String message) {
        log.info("User Registered: {}", message);
        try {
            UserRegisteredEvent event = objectMapper.readValue(message, UserRegisteredEvent.class);
            log.info(
                    "User Registered - name: {} currency: {} email: {}",
                    event.getName(),
                    event.getCurrency(),
                    event.getEmail()
            );
            accountService.createAccount(event.getId(), event.getName(), event.getCurrency());
        } catch (Exception e) {
            log.error("Failed to process user registration: {}", e.getMessage());
        }
    }
}