package com.battybuilds.webclientoauth2.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ClientHttpConnector;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.security.oauth2.client.AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager;
import org.springframework.security.oauth2.client.ClientCredentialsReactiveOAuth2AuthorizedClientProvider;
import org.springframework.security.oauth2.client.InMemoryReactiveOAuth2AuthorizedClientService;
import org.springframework.security.oauth2.client.endpoint.WebClientReactiveClientCredentialsTokenResponseClient;
import org.springframework.security.oauth2.client.registration.ClientRegistration;
import org.springframework.security.oauth2.client.registration.InMemoryReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.registration.ReactiveClientRegistrationRepository;
import org.springframework.security.oauth2.client.web.reactive.function.client.ServerOAuth2AuthorizedClientExchangeFilterFunction;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;
import reactor.netty.tcp.TcpClient;
import reactor.netty.transport.ProxyProvider;

@Configuration
public class OAuth2WebClientConfig {

    private static final String AZURE_TOKEN_URI = "https://login.microsoftonline.com/azure.onmicrosoft.com/oauth2/token";
    private static final String XYZ_REGISTRATION_ID = "xyz";
    private static final String ABC_REGISTRATION_ID = "abc";
    private final String azureClientId;
    private final String azureClientSecret;
    private final String abcBaseUrl;
    private String springProfile;
    private final String xyzResource;
    private int xyzWebClientMaxMemorySize;
    private int abcWebClientMaxMemorySize;
    private String xyzBaseUrl;
    private String abcResource;

    public OAuth2WebClientConfig(@Value("${xyz-resource}") String xyzResource,
                                 @Value("${azure-client-id}") String azureClientId,
                                 @Value("${azure-client-secret}") String azureClientSecret,
                                 @Value("${abc-base-url}") String abcBaseUrl,
                                 @Value("${xyz-base-url}") String xyzBaseUrl,
                                 @Value("${spring.profiles.active}") String profile,
                                 @Value("${xyz-webClient-max-in-memory-size}") int xyzWebClientMaxMemorySize,
                                 @Value("${abc-webClient-max-in-memory-size}") int abcWebClientMaxMemorySize,
                                 @Value("${abc-resource}") String abcResource)    {
        this.xyzResource = xyzResource;
        this.xyzBaseUrl = xyzBaseUrl;
        this.abcResource = abcResource;
        this.azureClientId = azureClientId;
        this.azureClientSecret = azureClientSecret;
        this.abcBaseUrl = abcBaseUrl;
        springProfile = profile;
        this.xyzWebClientMaxMemorySize = xyzWebClientMaxMemorySize;
        this.abcWebClientMaxMemorySize = abcWebClientMaxMemorySize;
    }

    @Bean(name = "abcWebClient")
    WebClient abcWebClient(ReactiveClientRegistrationRepository clientRegistrations) {
        AzureTokenResponseClient azureTokenResponseClient =
                new AzureTokenResponseClient(abcResource, connectorForProxyAndTimeout());

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(abcWebClientMaxMemorySize)).build();

        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(abcBaseUrl)
                .clientConnector(connectorForProxyAndTimeout())
                .filter(setUpOAuth2(clientRegistrations, azureTokenResponseClient, ABC_REGISTRATION_ID))
                .build();
    }

    @Bean(name = "xyzWebClient")
    WebClient xyzWebClient(ReactiveClientRegistrationRepository clientRegistrations) {
        AzureTokenResponseClient azureTokenResponseClient =
                new AzureTokenResponseClient(xyzResource, connectorForProxyAndTimeout());

        ExchangeStrategies exchangeStrategies = ExchangeStrategies.builder()
                .codecs(configurer -> configurer.defaultCodecs().maxInMemorySize(xyzWebClientMaxMemorySize)).build();

        return WebClient.builder()
                .exchangeStrategies(exchangeStrategies)
                .baseUrl(xyzBaseUrl)
                .clientConnector(connectorForProxyAndTimeout())
                .filter(setUpOAuth2(clientRegistrations, azureTokenResponseClient, XYZ_REGISTRATION_ID))
                .build();
    }

    @Bean
    ReactiveClientRegistrationRepository clientRegistrations() {
        return new InMemoryReactiveClientRegistrationRepository(xyzCredentials(), abcCredentials());
    }

    private ClientRegistration xyzCredentials() {
        return ClientRegistration
                .withRegistrationId(XYZ_REGISTRATION_ID)
                .tokenUri(AZURE_TOKEN_URI)
                .clientId(azureClientId)
                .clientSecret(azureClientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
    }

    private ClientRegistration abcCredentials() {
        return ClientRegistration
                .withRegistrationId(ABC_REGISTRATION_ID)
                .tokenUri(AZURE_TOKEN_URI)
                .clientId(azureClientId)
                .clientSecret(azureClientSecret)
                .authorizationGrantType(AuthorizationGrantType.CLIENT_CREDENTIALS)
                .build();
    }

    private ServerOAuth2AuthorizedClientExchangeFilterFunction setUpOAuth2(ReactiveClientRegistrationRepository clientRegistrations,
                                                                           WebClientReactiveClientCredentialsTokenResponseClient tokenResponseClient,
                                                                           String clientRegistrationId) {
        InMemoryReactiveOAuth2AuthorizedClientService clientService =
                new InMemoryReactiveOAuth2AuthorizedClientService(clientRegistrations);

        AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager clientManager =
                new AuthorizedClientServiceReactiveOAuth2AuthorizedClientManager(clientRegistrations, clientService);
        clientManager.setAuthorizedClientProvider(constructProvider(tokenResponseClient));
        ServerOAuth2AuthorizedClientExchangeFilterFunction oauth =
                new ServerOAuth2AuthorizedClientExchangeFilterFunction(clientManager);

        oauth.setDefaultClientRegistrationId(clientRegistrationId);
        return oauth;
    }

    private ClientCredentialsReactiveOAuth2AuthorizedClientProvider constructProvider(
            WebClientReactiveClientCredentialsTokenResponseClient tokenResponseClient) {
        final ClientCredentialsReactiveOAuth2AuthorizedClientProvider provider =
                new ClientCredentialsReactiveOAuth2AuthorizedClientProvider();

        provider.setAccessTokenResponseClient(tokenResponseClient);
        return provider;
    }

    private ClientHttpConnector connectorForProxyAndTimeout() {
        HttpClient httpClient = HttpClient.create()
                .tcpConfiguration(client ->
                {
                    TcpClient tcpClient = client.option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 60000)
                            .doOnConnected(conn -> conn
                                    .addHandlerLast(new ReadTimeoutHandler(60))
                                    .addHandlerLast(new WriteTimeoutHandler(60))
                            );
                    if ("local".equals(springProfile)) {
                        tcpClient = tcpClient.proxy(proxy -> proxy.type(ProxyProvider.Proxy.HTTP)
                                .host("internet.dorf.com")
                                .port(83)
                        );
                    }
                    return tcpClient;
                });
        return new ReactorClientHttpConnector(httpClient.wiretap(true));
    }
}
