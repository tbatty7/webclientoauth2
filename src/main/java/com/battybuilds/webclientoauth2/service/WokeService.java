package com.battybuilds.webclientoauth2.service;


import com.battybuilds.webclientoauth2.AlarmRequest;
import com.battybuilds.webclientoauth2.WokeResponse;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Duration;

@Service
public class WokeService {
    private final WebClient webClient;

    public WokeService(WebClient wokeWebClient) {
        this.webClient = wokeWebClient;
    }

    public WokeResponse getAlarms() {
        Mono<WokeResponse> wokeResponseMono = webClient.get()
                .uri("/api/clock/alarms")
                .header("Identification-Id", "1234")
                .retrieve()
                .bodyToMono(WokeResponse.class);

        return wokeResponseMono.block(Duration.ofSeconds(30));
    }

    public WokeResponse addAlarm(AlarmRequest requestBody) {
        Mono<WokeResponse> wokeResponseMono = webClient.post()
                .uri("/api/clock/alarms")
                .header("Identification-Id", "1234")
                .body(BodyInserters.fromValue(requestBody))
                .retrieve()
                .bodyToMono(WokeResponse.class);

        return wokeResponseMono.block(Duration.ofSeconds(30));
    }
}