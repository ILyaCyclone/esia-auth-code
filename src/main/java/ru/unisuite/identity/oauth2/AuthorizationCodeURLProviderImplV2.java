package ru.unisuite.identity.oauth2;

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.service.EsiaAccessException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;

@Service
@Primary
public class AuthorizationCodeURLProviderImplV2 implements AuthorizationCodeURLProvider {
    private static final Logger logger = LoggerFactory.getLogger(AuthorizationCodeURLProviderImplV2.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss Z")
            .withZone(ZoneId.systemDefault());

    private final CryptoSigner cryptoSigner;
    private final EsiaProperties esiaProperties;

    private final String scope;
    private final UriComponentsBuilder baseAuthCodeUriBuilder;

    AuthorizationCodeURLProviderImplV2(CryptoSigner cryptoSigner, EsiaProperties esiaProperties) {

        this.cryptoSigner = cryptoSigner;
        this.esiaProperties = esiaProperties;


        this.scope = esiaProperties.getScopesString();


        baseAuthCodeUriBuilder = UriComponentsBuilder.fromHttpUrl(esiaProperties.getEsiaBaseUrl() + "/aas/oauth2/v2/ac")
                .queryParam("client_id", esiaProperties.getClientId())
                .queryParam("client_certificate_hash", esiaProperties.getClientCertificateHash())
                .queryParam("scope", scope)
                .queryParam("response_type", "code")
                .queryParam("access_type", "offline"); // "offline" or "online"
    }


    @Override
    public String generateAuthorizationCodeURL(String returnUrl) {
        try {
            String timestamp = generateTimestamp();
            String clientId = esiaProperties.getClientId();
            String state = generateState();
            String clientSecret = generateClientSecret(ClientSecretParameters.builder()
                    .clientId(clientId).scope(scope).timestamp(timestamp).state(state).redirectUrl(returnUrl)
                    .build());

            UriComponentsBuilder authCodeUriBuilder = baseAuthCodeUriBuilder.cloneBuilder()
                    .queryParam("client_secret", clientSecret)
                    .queryParam("state", state);

            String url = authCodeUriBuilder.toUriString();
            url += "&timestamp=" + urlEncode(timestamp);
            url += "&redirect_uri=" + urlEncode(returnUrl);

            logger.debug("authentication code url: {}", url);

            return url;
        } catch (Exception e) {
            throw new EsiaAccessException("Unable to generate access token url", e);
        }
    }


    private String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new EsiaAccessException("Could not encode string '" + string + '\'', e);
        }
    }


    private String generateState() {
        return UUID.randomUUID().toString();
    }

    private String generateTimestamp() {
        return dateTimeFormatter.format(Instant.now());
    }

    private String generateClientSecret(ClientSecretParameters p) {
        String[] toJoin = {p.clientId, p.scope, p.timestamp, p.state, p.redirectUrl};
        String clientSecretUnsigned = String.join("", toJoin);
        logger.debug("clientSecret unsigned: {}", clientSecretUnsigned);

        byte[] signedClientSecretBytes = cryptoSigner.signGost2012(clientSecretUnsigned);
        return Base64.getUrlEncoder().encodeToString(signedClientSecretBytes);
    }

    @Builder
    static class ClientSecretParameters {
        private final String clientId;
        private final String scope;
        private final String timestamp;
        private final String state;
        private final String redirectUrl;
        private final String authorizationCode;
    }

}
