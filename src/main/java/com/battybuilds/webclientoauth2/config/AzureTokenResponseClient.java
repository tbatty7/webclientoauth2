package com.battybuilds.webclientoauth2.config;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.security.oauth2.client.endpoint.OAuth2ClientCredentialsGrantRequest;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.core.endpoint.OAuth2AccessTokenResponse;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.util.Assert;
import org.springframework.web.reactive.function.BodyInserters;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.Collections;

import static org.springframework.security.oauth2.core.web.reactive.function.OAuth2BodyExtractors.oauth2AccessTokenResponse;

public class AzureTokenResponseClient extends WebClientReactiveClientCredentialsTokenResponseClient {

    private WebClient webClient;
    private String resource;

    public AzureTokenResponseClient(String resource, ClientHttpConnector loyaltyConnector) {
        this.resource = resource;
        webClient = WebClient.builder().clientConnector(loyaltyConnector).build();
    }

    @Override
    public Mono<OAuth2AccessTokenResponse> getTokenResponse(OAuth2ClientCredentialsGrantRequest grantRequest) {
        Assert.notNull(grantRequest, "grantRequest cannot be null");
        return Mono.defer(() -> webClient.post()
                .uri(grantRequest.getClientRegistration().getProviderDetails().getTokenUri())
                .headers(this::populateTokenRequestHeaders)
                .body(createTokenRequestBody(grantRequest))
                .exchange()
                .flatMap(response -> response.body(oauth2AccessTokenResponse())));
    }

    private void populateTokenRequestHeaders(HttpHeaders headers) {
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        headers.setAccept(Collections.singletonList(MediaType.APPLICATION_JSON));
    }

    private BodyInserters.FormInserter<String> createTokenRequestBody(OAuth2ClientCredentialsGrantRequest grantRequest) {
        BodyInserters.FormInserter<String> body = BodyInserters
                .fromFormData(OAuth2ParameterNames.GRANT_TYPE, grantRequest.getGrantType().getValue());
        ClientRegistration clientRegistration = grantRequest.getClientRegistration();
        body.with(OAuth2ParameterNames.CLIENT_ID, clientRegistration.getClientId());
        body.with(OAuth2ParameterNames.CLIENT_SECRET, clientRegistration.getClientSecret());
        body.with("resource", resource);
        return body;
    }
}
