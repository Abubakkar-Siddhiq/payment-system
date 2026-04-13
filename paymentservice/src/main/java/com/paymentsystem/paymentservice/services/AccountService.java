package com.paymentsystem.paymentservice.services;

import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.exception.PaymentException;
import com.paymentsystem.paymentservice.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.paymentsystem.paymentservice.domain.enums.Currency;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final RedisTemplate<String, String> redisTemplate;

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(UUID id) {
        return accountRepository.findByIdWithLock(id).orElseThrow(() -> new EntityNotFoundException("Account Not Found: "+ id));
    }



    public Account createAccount(String owner, Currency currency) {
        Account account = new Account();
        account.setOwner(owner);
        account.setCurrency(currency);
        return accountRepository.save(account);
    }

    @Transactional
    public Account deposit(UUID id, BigDecimal amount, String idkey) {
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

            return accountRepository.findById(UUID.fromString(value))
                    .orElseThrow(() -> new PaymentException("Account not found"));
        }

        Account account = this.getAccountById(id);
        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);

        redisTemplate.opsForValue().set(idkey, saved.getId().toString(), 24, TimeUnit.HOURS);

        return saved;
    }
}
