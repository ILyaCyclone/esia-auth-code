package ru.unisuite.identity;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.ConstructorBinding;
import ru.unisuite.identity.oauth2.Scope;

import java.time.Duration;
import java.util.List;
import java.util.stream.Collectors;

@ConfigurationProperties(prefix = "esia")
@ConstructorBinding
@Data
public class EsiaProperties {

    private final String baseUrl; // this service base url, not esia

    private final Environment environment;

    private final List<Scope> scopes;

    /**
     * scopes as string (lowercased, separated with space), for use as http parameter
     */
    public String getScopesString() {
        return getScopes().stream()
                .map(streamScope -> streamScope.toString().toLowerCase())
                .collect(Collectors.joining(" "));
    }

    public String getEsiaBaseUrl() {
        return environment.baseUrl;
    }

    public String getIssuer() {
        return environment.issuer;
    }

    private final String issuer;
    private final String certificatePath;

    private final Duration connectTimeout;
    private final Duration readTimeout;
    private final int jwtAllowedClockSkewSeconds;

    private final String clientId;
    private final String clientCertificateHash;

    private final String keystoreAlias;
    private final String keystorePassword;

    enum Environment {
        PROD("https://esia.gosuslugi.ru", "http://esia.gosuslugi.ru/"),
        TEST("https://esia-portal1.test.gosuslugi.ru", "http://esia-portal1.test.gosuslugi.ru/");

        private final String baseUrl;
        private final String issuer;

        Environment(String baseUrl, String issuer) {
            this.baseUrl = baseUrl;
            this.issuer = issuer;
        }
    }
}
