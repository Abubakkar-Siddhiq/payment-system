package com.paymentsystem.paymentservice.controllers;

import com.paymentsystem.paymentservice.domain.dtos.request.AccountRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.CreateAccountRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.request.DepositRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.DepositRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.response.AccountBalanceResponse;
import com.paymentsystem.paymentservice.domain.dtos.response.AccountResponse;
import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.mappers.AccountMapper;
import com.paymentsystem.paymentservice.services.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/accounts")
@RequiredArgsConstructor
public class AccountController {

    public final AccountService accountService;
    private final AccountMapper accountMapper;

    @GetMapping
    public ResponseEntity<List<AccountResponse>> listAccounts() {
        List<AccountResponse> accounts = accountService.listAccounts().stream().map(accountMapper::toAccountResponse).toList();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id
            ) {
        Account account = accountService.getAccountById(id);
        AccountResponse res = accountMapper.toAccountResponse(account);
        return ResponseEntity.ok(res);
    }

    @PostMapping
    public ResponseEntity<AccountResponse> createAccount(
            @Valid @RequestBody CreateAccountRequestDto createAccountRequestDto
            ) {
        AccountRequest accountRequest = accountMapper.toAccountRequest(createAccountRequestDto);
        Account account = accountService.createAccount(accountRequest.getOwner(), accountRequest.getCurrency());
        AccountResponse res = accountMapper.toAccountResponse(account);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @PostMapping(path = "/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID id,
            @RequestBody DepositRequestDto depositRequestDto
            ) {
        DepositRequest depositRequest = accountMapper.toDepositRequest(depositRequestDto);
        Account account = accountService.deposit(id, depositRequest.getAmount());
        AccountResponse res = accountMapper.toAccountResponse(account);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping(path = "/{id}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable UUID id
    ) {
        Account account = accountService.getAccountById(id);
        AccountBalanceResponse res = accountMapper.toAccountBalanceResponse(account);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
