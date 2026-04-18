package com.paymentsystem.paymentservice.outbox;

import com.paymentsystem.paymentservice.kafka.producers.PaymentEventProducer;
import com.paymentsystem.paymentservice.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
@Slf4j
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final PaymentEventProducer producer;

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalse();

        for (OutboxEvent event : pending) {
            producer.publishPaymentEvent(event.getPayload());
            event.setPublished(true);
            outboxRepository.save(event);
        }
    }
}