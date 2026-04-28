package com.paymentsystem.frauddetectionsystem.controllers;


import com.paymentsystem.frauddetectionsystem.domain.dto.FraudAlertResponse;
import com.paymentsystem.frauddetectionsystem.domain.entity.FraudAlert;
import com.paymentsystem.frauddetectionsystem.mappers.FraudAlertMapper;
import com.paymentsystem.frauddetectionsystem.repositories.FraudAlertRepository;
import com.paymentsystem.frauddetectionsystem.services.FraudService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping(path = "/fraud/alerts")
@RequiredArgsConstructor
public class FraudController {

    private final FraudAlertRepository fraudAlertRepository;
    private final FraudAlertMapper fraudAlertMapper;
    private final FraudService fraudService;

    @GetMapping
    public ResponseEntity<List<FraudAlertResponse>> getAlerts() {
        List<FraudAlertResponse> alerts = fraudAlertRepository.findAll()
                .stream()
                .map(fraudAlertMapper::toFraudAlertResponse)
                .toList();

        return ResponseEntity.ok(alerts);
    }

    @GetMapping("/{id}")
    public ResponseEntity<FraudAlertResponse> getAlert(
            @PathVariable UUID id
    ) {

        return fraudAlertRepository.findById(id)
                .map(fraudAlertMapper::toFraudAlertResponse)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<FraudAlertResponse> approve(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String reviewedBy
    ) {
        FraudAlert alert = fraudService.approve(id, reviewedBy);
        return ResponseEntity.ok(fraudAlertMapper.toFraudAlertResponse(alert));
    }

    @PutMapping("/{id}/reject")
    public ResponseEntity<FraudAlertResponse> reject(
            @PathVariable UUID id,
            @RequestHeader("X-User-Id") String reviewedBy
    ) {
        FraudAlert alert = fraudService.reject(id, reviewedBy);
        return ResponseEntity.ok(fraudAlertMapper.toFraudAlertResponse(alert));
    }
}
