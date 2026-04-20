package com.paymentsystem.frauddetectionsystem.mappers;

import com.paymentsystem.frauddetectionsystem.domain.dto.FraudAlertResponse;
import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface FraudAlertMapper {
    FraudAlertResponse toFraudAlertResponse(FraudAlert fraudAlert);
}
