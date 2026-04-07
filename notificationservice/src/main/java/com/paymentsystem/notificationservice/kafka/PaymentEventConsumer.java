package com.paymentsystem.notificationservice.kafka;

import com.paymentsystem.notificationservice.kafka.event.TransactionEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.json.JsonParseException;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.stereotype.Component;
import tools.jackson.databind.ObjectMapper;


@Component
@RequiredArgsConstructor
@Slf4j
public class PaymentEventConsumer {

    private final ObjectMapper objectMapper;

    @KafkaListener(topics = "payment-topic", groupId = "notification-group")
    public void consume(String message) {
        try {
            TransactionEvent event = objectMapper.readValue(message, TransactionEvent.class);
            log.info("Payment of {} from {} to {} - {} at: {}",
                    event.getAmount(), event.getSenderId(),
                    event.getReceiverId(), event.getStatus(),
                    event.getTimestamp());
        } catch (Exception e) {
            log.error("Failed to deserialize payment event: {}", e.getMessage());
        }
    }
}
