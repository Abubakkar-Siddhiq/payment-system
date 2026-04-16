package com.paymentsystem.authservice.service;

import com.paymentsystem.authservice.domain.entity.User;
import com.paymentsystem.authservice.domain.enums.Currency;
import com.paymentsystem.authservice.kafka.event.UserRegisterdProducer;
import com.paymentsystem.authservice.kafka.event.UserRegisteredEvent;
import com.paymentsystem.authservice.mappers.UserMapper;
import com.paymentsystem.authservice.repositories.UserRepository;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final UserDetailsService userDetailsService;
    private final UserRegisterdProducer userRegisterdProducer;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;

    public String generateToken(UserDetails userDetails) {
        // 24hrs
        long jwtExpiryMs = 86400000L;
        return Jwts.builder()
                .subject(userDetails.getUsername())
                .issuedAt(new Date(System.currentTimeMillis()))
                .expiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    public UserDetails validateToken(String token) {
        String username = extractUsername(token);
        return userDetailsService.loadUserByUsername(username);
    }

    private String extractUsername(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return claims.getSubject();
    }

    private SecretKey getSigningKey() {
        byte[] keyBytes = Decoders.BASE64.decode(SECRET_KEY);
        return Keys.hmacShaKeyFor(keyBytes);
    }

    public UserDetails authenticate(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        return userDetailsService.loadUserByUsername(email);
    }

    public UserDetails register(String name, String email, String password, Currency currency) {

        // Normalize email (important)
        email = email.toLowerCase().trim();

        // 1. Check if user already exists
        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists with email: " + email);
        }

        try {
            // 2. Create user
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(password);
            user.setCurrency(currency);

            User saved = userRepository.save(user);

            // 3. Publish event
            UserRegisteredEvent event = userMapper.toUserRegisteredEvent(saved);
            String eventMsg = objectMapper.writeValueAsString(event);
            userRegisterdProducer.publishEvent(eventMsg);

            // 4. Return user details
            return userDetailsService.loadUserByUsername(email);

        } catch (DataIntegrityViolationException e) {
            // Handles race condition (2 requests at same time)
            throw new RuntimeException("User already exists with email: " + email);
        } catch (Exception e) {
            throw new RuntimeException("Registration failed", e);
        }
    }

}