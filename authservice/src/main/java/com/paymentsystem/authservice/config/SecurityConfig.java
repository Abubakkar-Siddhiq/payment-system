package com.paymentsystem.authservice.config;

import com.paymentsystem.authservice.domain.entity.User;
import com.paymentsystem.authservice.repositories.UserRepository;
import com.paymentsystem.authservice.security.JwtAuthenticationFilter;
import com.paymentsystem.authservice.service.AuthService;
import com.paymentsystem.authservice.security.AuthUserDetailsService;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

@Configuration
public class SecurityConfig {

    @Bean
    public JwtAuthenticationFilter jwtAuthenticationFilter(AuthService authenticationService) {
        return new JwtAuthenticationFilter(authenticationService);
    }

//    @Bean
//    public UserDetailsService userDetailsService(UserRepository userRepository) {
//        AuthUserDetailsService blogUserDetailsService = new AuthUserDetailsService(userRepository);
//
//        String email = "user@test.com";
//        userRepository.findByEmail(email).orElseGet(() -> {
//            User newUser = User.builder().name("Test User")
//                    .email(email)
//                    .password(passwordEncoder().encode("password"))
//                    .build();
//
//            return userRepository.save(newUser);
//        });
//
//        return blogUserDetailsService;
//    }

    @Bean
    public SecurityFilterChain securityFilterChain(
            HttpSecurity http,
            JwtAuthenticationFilter jwtAuthenticationFilter
    ) throws Exception {
        http
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/v1/auth/register").permitAll()
                        .anyRequest().authenticated()
                )
                .csrf(csrf -> csrf.disable())
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
                )
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return PasswordEncoderFactories.createDelegatingPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration config) throws Exception {
        return config.getAuthenticationManager();
    }
}
