package com.paymentsystem.paymentservice.controllers;

import com.paymentsystem.paymentservice.domain.dtos.request.TransactionRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.TransactionRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.response.TransactionResponse;
import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.domain.entity.Transaction;
import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
import com.paymentsystem.paymentservice.mappers.TransactionMapper;
import com.paymentsystem.paymentservice.services.AccountService;
import com.paymentsystem.paymentservice.services.TransactionService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping(path = "/api/v1/payments")
@RequiredArgsConstructor
public class TransactionController {

    private final TransactionService transactionService;
    private final TransactionMapper transactionMapper;
    private final AccountService accountService;

    @PostMapping
    public ResponseEntity<TransactionResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idkey,
            @Valid  @RequestBody TransactionRequestDto transactionRequestDto
    ) {

        TransactionRequest transactionRequest = transactionMapper.toTransactionRequest(transactionRequestDto);

        Account sender = accountService.getAccountById(transactionRequest.getSender());
        Account receiver = accountService.getAccountById(transactionRequest.getReceiver());

        if(sender == null || receiver == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Transaction transaction = transactionService.processPayment(
                sender,
                receiver,
                transactionRequest.getAmount(),
                idkey
        );

        TransactionResponse res = transactionMapper.toTransactionResponse(transaction);

        if(transaction.getStatus() == TransactionStatus.SUCCESS) {
            return new ResponseEntity<>(res, HttpStatus.CREATED);
        }

        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }
}
