package ru.unisuite.identity.config;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;
import ru.unisuite.identity.EsiaProperties;

import java.time.Duration;
import java.util.Optional;

@Configuration
public class ApplicationConfig {

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(30);
    private static final Duration DEFAULT_READ_TIMEOUT = Duration.ofSeconds(30);

    @Autowired
    private EsiaProperties esiaProperties;

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder
                .setConnectTimeout(Optional.ofNullable(esiaProperties.getConnectTimeout()).orElse(DEFAULT_CONNECT_TIMEOUT))
                .setReadTimeout(Optional.ofNullable(esiaProperties.getReadTimeout()).orElse(DEFAULT_READ_TIMEOUT))
                .build();
    }
}
