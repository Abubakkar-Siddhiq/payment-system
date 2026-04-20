package com.paymentsystem.frauddetectionsystem.controllers;


import com.paymentsystem.frauddetectionsystem.domain.dto.FraudAlertResponse;
import com.paymentsystem.frauddetectionsystem.mappers.FraudAlertMapper;
import com.paymentsystem.frauddetectionsystem.repositories.FraudAlertRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/api/v1/fraud/alerts")
@RequiredArgsConstructor
public class FraudController {

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertMapper fraudAlertMapper;

    @GetMapping
    public ResponseEntity<List<FraudAlertResponse>> getAlerts() {
        List<FraudAlertResponse> alerts = fraudAlertRepository.findAll()
                .stream()
                .map(fraudAlertMapper::toFraudAlertResponse)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudAlertResponse> getAlert(@PathVariable UUID id) {

        return fraudAlertRepository.findById(id)
                .map(fraudAlertMapper::toFraudAlertResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

}
