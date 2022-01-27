package com.battybuilds.webclientoauth2.service;

import com.battybuilds.webclientoauth2.WokeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class SecureAbcService {

    private final WebClient webClient;

    public SecureAbcService(WebClient abcWebClient) {
        this.webClient = abcWebClient;
    }

    public WokeResponse getAlarms() {
        Mono<WokeResponse> wokeResponseMono = webClient.get()
                .uri("/api/clock/alarms")
                .header("Identification-Id", "1234")
                .retrieve()
                .bodyToMono(WokeResponse.class);

        return wokeResponseMono.block(Duration.ofSeconds(30));
    }
}
