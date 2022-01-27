package com.battybuilds.webclientoauth2.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    public WebClientConfig() {
    }

    @Bean(name = "wokeWebClient")
    WebClient webClient() {
        return WebClient.builder()
                .build();
    }

}