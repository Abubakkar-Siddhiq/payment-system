package com.paymentsystem.paymentservice.services;

import com.paymentsystem.paymentservice.domain.dtos.response.AccountBalanceResponse;
import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.exception.PaymentException;
import com.paymentsystem.paymentservice.mappers.AccountMapper;
import com.paymentsystem.paymentservice.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.paymentsystem.paymentservice.domain.enums.Currency;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;
    private final AccountMapper accountMapper;
    private final RedisTemplate<String, String> redisTemplate;

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Account Not Found: "+ id));
    }

    @Transactional
    public Account getAccountByIdWithLock(UUID id) {
        return accountRepository.findByIdWithLock(id).orElseThrow(() -> new EntityNotFoundException("Account Not Found: "+ id));
    }

    public void createAccount(UUID id, String owner, Currency currency) {
        if(accountRepository.existsById(id)) {
            log.info("Account already exits");
            return;
        }

        try {
            Account account = new Account();
            account.setId(id);
            account.setOwner(owner);
            account.setCurrency(currency);
            accountRepository.save(account);

            log.info("Account Created {}", account);
        }
        catch (DataIntegrityViolationException e) {
            log.warn("Account already exists for user: {}", id);
        }
    }

    @Transactional
    public Account deposit(String accountNumber, BigDecimal amount, String idkey, String userId) {
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


        Account account = accountRepository.findByNumberWithLock(accountNumber)
                .orElseThrow(() -> new PaymentException("Account not found"));

        if (!account.getId().toString().equals(userId)) {
            throw new PaymentException("Unauthorized - account does not belong to you");
        }

        account.setBalance(account.getBalance().add(amount));
        Account saved = accountRepository.save(account);

        redisTemplate.opsForValue().set(idkey, saved.getId().toString(), 24, TimeUnit.HOURS);

        return saved;
    }

    public AccountBalanceResponse getBalance(String accountNumber, String userId) {
        Account account = accountRepository.findByNumber(accountNumber)
                .orElseThrow(() -> new PaymentException("Account not found"));

        if (!account.getId().toString().equals(userId)) {
            throw new PaymentException("Unauthorized - account does not belong to you");
        }

        return accountMapper.toAccountBalanceResponse(account);
    }

}
