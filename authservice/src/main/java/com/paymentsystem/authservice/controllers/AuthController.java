package com.paymentsystem.authservice.controllers;

import com.paymentsystem.authservice.domain.dtos.request.LoginRequest;
import com.paymentsystem.authservice.domain.dtos.request.RegisterRequest;
import com.paymentsystem.authservice.domain.dtos.response.AuthResponse;
import com.paymentsystem.authservice.domain.entity.User;
import com.paymentsystem.authservice.service.AuthService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping(path = "/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authenticationService;

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(
            @RequestBody LoginRequest loginRequest
    ) {
        User user = authenticationService.authenticate(loginRequest.getEmail(), loginRequest.getPassword());
        String tokenValue =  authenticationService.generateToken(user);

        AuthResponse authResponse =AuthResponse.builder()
                .token(tokenValue)
                .expiresIn(86400)
                .build();

        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("register")
    public ResponseEntity<AuthResponse> register(
            @RequestBody RegisterRequest registerRequest
    ) {
        User user = authenticationService.register(registerRequest.getName(), registerRequest.getEmail(), registerRequest.getPassword(), registerRequest.getCurrency());
        String tokenValue =  authenticationService.generateToken(user);

        AuthResponse authResponse =AuthResponse.builder()
                .token(tokenValue)
                .expiresIn(86400)
                .build();

        return ResponseEntity.ok(authResponse);
    }
}