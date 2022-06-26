package ru.unisuite.identity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.oauth2.AccessTokenProviderImplV1;
import ru.unisuite.identity.oauth2.AuthorizationCodeURLProviderImplV2;
import ru.unisuite.identity.oauth2.Oauth2Flow;

import java.time.Duration;
import java.util.Optional;

@Configuration
public class ApplicationConfig {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(60);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(60);

    @Autowired
    private EsiaProperties esiaProperties;

    @Bean
    public Oauth2Flow oauth2Flow(AuthorizationCodeURLProviderImplV2 authorizationCodeURLProvider,
                                 AccessTokenProviderImplV1 accessTokenProvider) {
        return new Oauth2Flow(authorizationCodeURLProvider, accessTokenProvider);
    }

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Optional.ofNullable(esiaProperties.getConnectTimeout()).orElse(DEFAULT_CONNECT_TIMEOUT))
                .setReadTimeout(Optional.ofNullable(esiaProperties.getReadTimeout()).orElse(DEFAULT_READ_TIMEOUT))
                .build();
    }
}
