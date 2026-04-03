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
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class PaymentService {

    private RedisTemplate<String, TransactionStatus> redisTemplate;

    private TransactionRepository transactionRepository;
    private AccountRepository accountRepository;

    @Transactional
    public TransactionStatus processPayment(Account sender, Account receiver, BigDecimal amount, String idkey) {
        if(redisTemplate.opsForValue().get(idkey) != null) {
            return redisTemplate.opsForValue().get(idkey);
        }

        redisTemplate.opsForValue().set(idkey, TransactionStatus.PENDING, 24, TimeUnit.HOURS);

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
            transactionRepository.save(transaction);

            redisTemplate.opsForValue().set(idkey, TransactionStatus.SUCCESS, 24, TimeUnit.HOURS);
        } else {
            redisTemplate.opsForValue().set(idkey, TransactionStatus.FAILED, 24, TimeUnit.HOURS);
            transaction.setStatus(TransactionStatus.FAILED);
            transactionRepository.save(transaction);
        }

        return transaction.getStatus();
    }
}
