package ru.unisuite.identity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;

import java.time.Duration;

@ConfigurationProperties(prefix = "esia")
@ConstructorBinding
@Data
public class EsiaProperties {
    private final String authCodeUrlV1;
    private final String authCodeUrlV2;

    private final String accessTokenUrlV1;
    private final String accessTokenUrlV3;

    private final String dataCollectionsUrl;

    private final String issuer;
    private final String certificatePath;

    private final Duration connectTimeout;
    private final Duration readTimeout;

    private final String clientId;
    private final String clientCertificateHash;
    private final String returnUrl;

    private final String keystoreAlias;
    private final String keystorePassword;

    private final String cabinetRedirectUrl;
}
