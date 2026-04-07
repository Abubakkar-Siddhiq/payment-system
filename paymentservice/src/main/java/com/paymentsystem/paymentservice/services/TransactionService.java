package com.paymentsystem.paymentservice.services;

import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.domain.entity.Transaction;
import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
import com.paymentsystem.paymentservice.exception.PaymentException;
import com.paymentsystem.paymentservice.kafka.PaymentEventProducer;
import com.paymentsystem.paymentservice.kafka.event.TransactionEvent;
import com.paymentsystem.paymentservice.repositories.AccountRepository;
import com.paymentsystem.paymentservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RedisTemplate<String, String> redisTemplate;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;
    private final PaymentEventProducer paymentEventProducer;
    private final ObjectMapper objectMapper;

    @Transactional
    public Transaction processPayment(UUID senderId, UUID receiverId, BigDecimal amount, String idkey) {
        Boolean locked = redisTemplate.opsForValue()
                .setIfAbsent(idkey, "PROCESSING", 10, TimeUnit.MINUTES);

        if (Boolean.FALSE.equals(locked)) {
            String value = redisTemplate.opsForValue().get(idkey);

            if (value == null) {
                throw new PaymentException("Invalid idempotency state");
            }

            if ("PROCESSING".equals(value)) {
                throw new PaymentException("Request already in progress");
            }

            return transactionRepository.findById(UUID.fromString(value))
                    .orElseThrow();
        }

        Account sender = accountRepository.findByIdWithLock(senderId)
                .orElseThrow(() -> new PaymentException("Sender account not found"));
        Account receiver = accountRepository.findByIdWithLock(receiverId)
                .orElseThrow(() -> new PaymentException("Receiver account not found"));

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setSender(sender);
        transaction.setReceiver(receiver);

        if(sender.getCurrency() != receiver.getCurrency()) {
            throw new PaymentException("Currency mismatch.");
        }

        if (sender.getBalance().compareTo(amount) < 0) {
            transaction.setStatus(TransactionStatus.FAILED);
            throw new PaymentException("Insufficient Balance");
        } else {
            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));

            accountRepository.save(sender);
            accountRepository.save(receiver);

            transaction.setStatus(TransactionStatus.SUCCESS);
        }

        Transaction saved = transactionRepository.save(transaction);
        redisTemplate.opsForValue().set(idkey, saved.getId().toString(), 24, TimeUnit.HOURS);

        TransactionEvent event = TransactionEvent.builder()
                .transactionId(saved.getId())
                .senderId(sender.getId())
                .receiverId(receiver.getId())
                .amount(amount)
                .status(saved.getStatus())
                .timestamp(saved.getTimestamp())
                .build();

        String eventMessage = objectMapper.writeValueAsString(event);
        paymentEventProducer.publishPaymentEvent(eventMessage);

        return saved;
    }
}
