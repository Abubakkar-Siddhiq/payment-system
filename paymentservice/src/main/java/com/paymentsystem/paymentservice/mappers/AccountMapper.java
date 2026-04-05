package com.paymentsystem.paymentservice.mappers;

import com.paymentsystem.paymentservice.domain.dtos.request.AccountRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.CreateAccountRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.request.DepositRequest;
import com.paymentsystem.paymentservice.domain.dtos.request.DepositRequestDto;
import com.paymentsystem.paymentservice.domain.dtos.response.AccountResponse;
import com.paymentsystem.paymentservice.domain.entity.Account;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface AccountMapper {
    AccountResponse toAccountResponse(Account account);
    AccountRequest toAccountRequest(CreateAccountRequestDto createAccountRequestDto);
    DepositRequest toDepositRequest(DepositRequestDto depositRequestDto);
}
