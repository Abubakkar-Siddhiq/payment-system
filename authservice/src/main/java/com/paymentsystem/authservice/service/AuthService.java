package com.paymentsystem.authservice.service;

import com.paymentsystem.authservice.domain.entity.User;
import com.paymentsystem.authservice.domain.enums.Currency;
import com.paymentsystem.authservice.kafka.event.UserRegisterdProducer;
import com.paymentsystem.authservice.kafka.event.UserRegisteredEvent;
import com.paymentsystem.authservice.mappers.UserMapper;
import com.paymentsystem.authservice.repositories.UserRepository;
import com.paymentsystem.authservice.security.AuthUserDetailsService;
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
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import javax.crypto.SecretKey;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AuthenticationManager authenticationManager;
    private final AuthUserDetailsService userDetailsService;
    private final UserRegisterdProducer userRegisterdProducer;

    @Value("${jwt.secret}")
    private String SECRET_KEY;

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;
    private final UserMapper userMapper;
    private final PasswordEncoder passwordEncoder;

    public String generateToken(User user) {
        long jwtExpiryMs = 86400000L;

        return Jwts.builder()
                .subject(user.getId().toString())
                .claim("email", user.getEmail())
                .claim("role", user.getRole().name())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + jwtExpiryMs))
                .signWith(getSigningKey())
                .compact();
    }

    public UserDetails validateToken(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(getSigningKey())
                .build()
                .parseSignedClaims(token)
                .getPayload();

        String username = claims.getSubject();

        if (username == null) {
            throw new RuntimeException("Invalid token");
        }

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

    public User authenticate(String email, String password) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, password)
        );
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));
    }

    public User register(String name, String email, String password, Currency currency) {
        email = email.toLowerCase().trim();

        if (userRepository.existsByEmail(email)) {
            throw new RuntimeException("User already exists with email: " + email);
        }

        try {
            User user = new User();
            user.setName(name);
            user.setEmail(email);
            user.setPassword(passwordEncoder.encode(password));
            user.setCurrency(currency != null ? currency : Currency.USD);

            User saved = userRepository.save(user);

            UserRegisteredEvent event = userMapper.toUserRegisteredEvent(saved);
            String eventMsg = objectMapper.writeValueAsString(event);
            userRegisterdProducer.publishEvent(eventMsg);

            return saved;

        } catch (DataIntegrityViolationException e) {
            throw new RuntimeException("User already exists with email: " + email);
        } catch (Exception e) {
            throw new RuntimeException("Registration failed", e);
        }
    }

}