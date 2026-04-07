package com.paymentsystem.paymentservice.controllers;

import com.paymentsystem.paymentservice.domain.dtos.request.TransactionRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.TransactionRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.response.TransactionResponse;
import com.paymentsystem.paymentservice.domain.entity.Transaction;
import com.paymentsystem.paymentservice.domain.enums.TransactionStatus;
import com.paymentsystem.paymentservice.mappers.TransactionMapper;
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

    @PostMapping
    public ResponseEntity<TransactionResponse> createPayment(
            @RequestHeader("Idempotency-Key") String idkey,
            @Valid  @RequestBody TransactionRequestDto transactionRequestDto
    ) {

        TransactionRequest transactionRequest = transactionMapper.toTransactionRequest(transactionRequestDto);

        if(transactionRequest.getSender() == null || transactionRequest.getReceiver() == null) {
            return new ResponseEntity<>(HttpStatus.NOT_FOUND);
        }

        Transaction transaction = transactionService.processPayment(
                transactionRequest.getSender(),
                transactionRequest.getReceiver(),
                transactionRequest.getAmount(),
                idkey
        );

        TransactionResponse res = transactionMapper.toTransactionResponse(transaction);

        if(transaction.getStatus() == TransactionStatus.SUCCESS) {
            res.setMessage("Transaction Completed Successfully.");
            return new ResponseEntity<>(res, HttpStatus.CREATED);
        }

        res.setMessage("Transaction Failed");
        return new ResponseEntity<>(res, HttpStatus.BAD_REQUEST);
    }
}
