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

        Account account = accountService.getAccountById(id);
        AccountResponse res = accountMapper.toAccountResponse(account);
        return ResponseEntity.ok(res);
    }

    @PostMapping(path = "/{id}/deposit")
    public ResponseEntity<AccountResponse> deposit(
            @PathVariable UUID id,
            @RequestHeader("Idempotency-Key") String idkey,
            @RequestHeader("X-User-Id") String userId,
            @RequestBody DepositRequestDto depositRequestDto
            ) {

        if (!id.toString().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        DepositRequest depositRequest = accountMapper.toDepositRequest(depositRequestDto);
        Account account = accountService.deposit(id, depositRequest.getAmount(), idkey);
        AccountResponse res = accountMapper.toAccountResponse(account);
        return new ResponseEntity<>(res, HttpStatus.CREATED);
    }

    @GetMapping(path = "/{id}/balance")
    public ResponseEntity<AccountBalanceResponse> getBalance(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String userId
    ) {

        if (!id.toString().equals(userId)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }

        Account account = accountService.getAccountById(id);
        AccountBalanceResponse res = accountMapper.toAccountBalanceResponse(account);
        return new ResponseEntity<>(res, HttpStatus.OK);
    }
}
