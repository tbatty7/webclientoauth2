package com.battybuilds.webclientoauth2.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.reactive.function.client.WebClient;

@Configuration
public class WebClientConfig {

    private final String baseUrl;

    public WebClientConfig(@Value("${base-url}") String baseUrl) {
        this.baseUrl = baseUrl;
    }

    @Bean(name = "wokeWebClient")
    WebClient webClient() {
        return WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

}