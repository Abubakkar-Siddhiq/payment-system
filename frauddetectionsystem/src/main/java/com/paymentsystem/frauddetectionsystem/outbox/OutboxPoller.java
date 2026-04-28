package com.paymentsystem.frauddetectionsystem.outbox;

import com.paymentsystem.frauddetectionsystem.kafka.FraudReviewEventProducer;
import com.paymentsystem.frauddetectionsystem.repositories.OutboxEventRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class OutboxPoller {

    private final OutboxEventRepository outboxRepository;
    private final FraudReviewEventProducer producer;

    @Scheduled(fixedDelay = 5000)
    public void processOutbox() {
        List<OutboxEvent> pending = outboxRepository.findByPublishedFalse();

        for (OutboxEvent event : pending) {
            producer.publishFraudReviewEvent(event.getPayload());
            event.setPublished(true);
            outboxRepository.save(event);
        }
    }

}
