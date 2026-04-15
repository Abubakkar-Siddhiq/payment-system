package com.paymentsystem.authservice.mappers;

import com.paymentsystem.authservice.domain.entity.User;
import com.paymentsystem.authservice.kafka.event.UserRegisteredEvent;
import org.mapstruct.Mapper;
import org.mapstruct.ReportingPolicy;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE)
public interface UserMapper {
    UserRegisteredEvent toUserRegisteredEvent(User user);
}
