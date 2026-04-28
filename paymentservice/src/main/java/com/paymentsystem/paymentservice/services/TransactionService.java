package com.paymentsystem.paymentservice.services;

import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.domain.entity.Transaction;
import com.paymentsystem.paymentservice.domain.enums.AccountStatus;
import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
import com.paymentsystem.paymentservice.exception.PaymentException;
import com.paymentsystem.paymentservice.kafka.event.TransactionEvent;
import com.paymentsystem.paymentservice.outbox.OutboxEvent;
import com.paymentsystem.paymentservice.repositories.AccountRepository;
import com.paymentsystem.paymentservice.repositories.OutboxEventRepository;
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
    private final ObjectMapper objectMapper;
    private final OutboxEventRepository outboxEventRepository;

    @Transactional
    public Transaction processPayment(String senderNo, String receiverNo, BigDecimal amount, String idkey, String userId) {
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
                    .orElseThrow(() -> new PaymentException("Transaction not Found."));
        }

        if(senderNo.equals(receiverNo)) {
            throw new PaymentException("Sender and Reciever can't be same");
        }

        Account sender = accountRepository.findByNumberWithLock(senderNo)
                .orElseThrow(() -> new PaymentException("Sender account not found"));
        Account receiver = accountRepository.findByNumberWithLock(receiverNo)
                .orElseThrow(() -> new PaymentException("Receiver account not found"));

        if (!sender.getId().toString().equals(userId)) {
            throw new PaymentException("Unauthorized - account does not belong to you");
        }

        if(sender.getStatus() == AccountStatus.FROZEN) {
            throw new PaymentException("Sender account is Frozen");
        }

        if(receiver.getStatus() == AccountStatus.FROZEN) {
            throw new PaymentException("Receiver account is Frozen");
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setSender(sender);
        transaction.setReceiver(receiver);

        if(sender.getCurrency() != receiver.getCurrency()) throw new PaymentException("Currency mismatch.");
        else transaction.setCurrency(sender.getCurrency());

        if (sender.getBalance().compareTo(amount) < 0) {
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
                .currency(saved.getCurrency())
                .timestamp(saved.getTimestamp())
                .build();

        String eventMessage = objectMapper.writeValueAsString(event);

        OutboxEvent outboxEvent = new OutboxEvent();
        outboxEvent.setPayload(eventMessage);
        outboxEventRepository.save(outboxEvent);

        return saved;
    }

    @Transactional
    public void freezeAccount(UUID transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException("Transaction not found"));

        Account sender = accountRepository.findByIdWithLock(original.getSender().getId())
                .orElseThrow();
        sender.setStatus(AccountStatus.FROZEN);
        accountRepository.save(sender);
    }

    @Transactional
    public void unfreezeAccount(UUID transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException("Transaction not found"));

        Account sender = accountRepository.findByIdWithLock(original.getSender().getId())
                .orElseThrow();

        // For Approving alerts below HIGH which won't be frozen.
        if(sender.getStatus() != AccountStatus.FROZEN) return;

        sender.setStatus(AccountStatus.ACTIVE);
        accountRepository.save(sender);
    }

    @Transactional
    public void reverseTransaction(UUID transactionId) {
        Transaction original = transactionRepository.findById(transactionId)
                .orElseThrow(() -> new PaymentException("Transaction not found"));

        Account sender = accountRepository.findByIdWithLock(original.getSender().getId())
                .orElseThrow();
        Account receiver = accountRepository.findByIdWithLock(original.getReceiver().getId())
                .orElseThrow();

        // Reverse the money
        sender.setBalance(sender.getBalance().add(original.getAmount()));
        receiver.setBalance(receiver.getBalance().subtract(original.getAmount()));

        // Freeze sender
        sender.setStatus(AccountStatus.FROZEN);

        accountRepository.save(sender);
        accountRepository.save(receiver);

        // Create reversal record in ledger
        Transaction reversal = new Transaction();
        reversal.setSender(original.getReceiver()); // flipped
        reversal.setReceiver(original.getSender()); // flipped
        reversal.setAmount(original.getAmount());
        reversal.setStatus(TransactionStatus.REVERSED);
        reversal.setCurrency(original.getCurrency());

        transactionRepository.save(reversal);
    }
}
