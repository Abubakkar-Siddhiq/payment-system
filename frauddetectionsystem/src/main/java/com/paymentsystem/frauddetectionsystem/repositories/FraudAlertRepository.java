package com.paymentsystem.frauddetectionsystem.repositories;

import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface FraudAlertRepository extends JpaRepository<FraudAlert, UUID> {
}
