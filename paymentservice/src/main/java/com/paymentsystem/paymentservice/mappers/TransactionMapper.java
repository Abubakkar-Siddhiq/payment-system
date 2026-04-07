package com.paymentsystem.paymentservice.mappers;

import com.paymentsystem.paymentservice.domain.dtos.request.TransactionRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.TransactionRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.response.TransactionResponse;
import com.paymentsystem.paymentservice.domain.entity.Account;
import com.paymentsystem.paymentservice.domain.entity.Transaction;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.Named;
import org.mapstruct.ReportingPolicy;

import java.util.UUID;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface TransactionMapper {
    TransactionRequest toTransactionRequest(TransactionRequestDto transactionRequestDto);

    @Mapping(target = "sender", source = "sender", qualifiedByName = "accountUUID")
    @Mapping(target = "receiver", source = "receiver", qualifiedByName = "accountUUID")
    TransactionResponse toTransactionResponse(Transaction transaction);

    @Named("accountUUID")
    default UUID accountUUID(Account account) {
        return account.getId();
    }
}
