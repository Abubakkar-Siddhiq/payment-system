package com.paymentsystem.paymentservice.services;

import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.domain.entity.Transaction;
import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
import com.paymentsystem.paymentservice.repositories.AccountRepository;
import com.paymentsystem.paymentservice.repositories.TransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class TransactionService {

    private final RedisTemplate<String, String> redisTemplate;

    private final TransactionRepository transactionRepository;
    private final AccountRepository accountRepository;

    @Transactional
    public Transaction processPayment(Account sender, Account receiver, BigDecimal amount, String idkey) {
        if(redisTemplate.opsForValue().get(idkey) != null) {
            String id = redisTemplate.opsForValue().get(idkey);
            assert id != null;
            Optional<Transaction> transaction = transactionRepository.findById(UUID.fromString(id));
            return transaction.orElse(null);
        }

        Transaction transaction = new Transaction();
        transaction.setAmount(amount);
        transaction.setSender(sender);
        transaction.setReceiver(receiver);

        if(sender.getBalance().compareTo(amount) >= 1) {
            sender.setBalance(sender.getBalance().subtract(amount));
            receiver.setBalance(receiver.getBalance().add(amount));

            accountRepository.save(sender);
            accountRepository.save(receiver);

            transaction.setStatus(TransactionStatus.SUCCESS);
        } else {
            transaction.setStatus(TransactionStatus.FAILED);
        }

        Transaction saved = transactionRepository.save(transaction);
        redisTemplate.opsForValue().set(idkey, saved.getId().toString(), 24, TimeUnit.HOURS);

        return saved;
    }
}
