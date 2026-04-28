package com.paymentsystem.paymentservice.controllers;

import com.paymentsystem.paymentservice.domain.dtos.request.DepositRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.DepositRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.response.AccountBalanceResponse;
import com.paymentsystem.paymentservice.domain.dtos.response.AccountResponse;
import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.mappers.AccountMapper;
import com.paymentsystem.paymentservice.services.AccountService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/accounts")
@RequiredArgsConstructor
public class AccountController {

    public final AccountService accountService;
    private final AccountMapper accountMapper;

    @GetMapping
    public ResponseEntity<List<Account>> listAccounts(
            @RequestHeader("X-User-Role") String role
    ) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        List<Account> accounts = accountService.listAccounts();
        return ResponseEntity.ok(accounts);
    }

    @GetMapping(path = "/{id}")
    public ResponseEntity<AccountResponse> getAccount(
            @PathVariable UUID id,
            @RequestHeader("X-User-Role") String role
            ) {

        if (!"ADMIN".equals(role)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Account account = accountService.getAccountByIdWithLock(id);
        AccountResponse res = accountMapper.toAccountResponse(account);
        return ResponseEntity.ok(res);
    }

    @PostMapping(path = "/{accountNumber}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable String accountNumber,
            @RequestHeader("Idempotency-Key") String idkey,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody DepositRequestDto depositRequestDto
            ) {
        DepositRequest depositRequest = accountMapper.toDepositRequest(depositRequestDto);
        Account account = accountService.deposit(accountNumber, depositRequest.getAmount(), idkey, userId);
        AccountResponse res = accountMapper.toAccountResponse(account);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping(path = "/{accountNumber}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable String accountNumber,
            @RequestHeader("X-User-Id") String userId
    ) {
        AccountBalanceResponse res = accountService.getBalance(accountNumber, userId);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }

    @GetMapping(path = "/me")
    public ResponseEntity<AccountResponse> getMeInfo(
            @RequestHeader("X-User-Id") String userId
    ) {
        Account account = accountService.getAccountById(UUID.fromString(userId));
        AccountResponse res = accountMapper.toAccountResponse(account);
        return ResponseEntity.ok(res);
    }
}
