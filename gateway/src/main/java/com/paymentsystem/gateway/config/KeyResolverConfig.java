package com.paymentsystem.gateway.config;

import org.springframework.cloud.gateway.filter.ratelimit.KeyResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import reactor.core.publisher.Mono;

import java.net.InetSocketAddress;

@Configuration
public class KeyResolverConfig {

    @Bean
    public KeyResolver ipKeyResolver() {
        return exchange -> {

            String xForwardedFor = exchange.getRequest()
                    .getHeaders()
                    .getFirst("X-Forwarded-For");

            if (xForwardedFor != null && !xForwardedFor.isBlank()) {
                return Mono.just(xForwardedFor.split(",")[0].trim());
            }

            InetSocketAddress remoteAddress =
                    exchange.getRequest().getRemoteAddress();

            if (remoteAddress != null &&
                    remoteAddress.getAddress() != null) {

                return Mono.just(
                        remoteAddress.getAddress().getHostAddress()
                );
            }

            return Mono.just("unknown");
        };
    }
}