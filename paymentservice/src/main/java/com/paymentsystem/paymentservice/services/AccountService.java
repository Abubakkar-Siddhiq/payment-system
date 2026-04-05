package com.paymentsystem.paymentservice.services;

import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.repositories.AccountRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import com.paymentsystem.paymentservice.domain.enums.Currency;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final AccountRepository accountRepository;

    public List<Account> listAccounts() {
        return accountRepository.findAll();
    }

    public Account getAccountById(UUID id) {
        return accountRepository.findById(id).orElseThrow(() -> new EntityNotFoundException("Account Not Found: "+ id));
    }

    public Account createAccount(String owner, Currency currency) {
        Account account = new Account();
        account.setOwner(owner);
        account.setCurrency(currency);
        return accountRepository.save(account);
    }

    public Account deposit(UUID id, BigDecimal amount) {
        Account account = this.getAccountById(id);
        BigDecimal balance = account.getBalance().add(amount);
        account.setBalance(balance);
        return accountRepository.save(account);
    }
}
