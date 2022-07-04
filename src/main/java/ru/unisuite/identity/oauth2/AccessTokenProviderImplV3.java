package ru.unisuite.identity.oauth2;

import lombok.Builder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.dto.AccessTokenDto;
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
public class AccessTokenProviderImplV3 implements AccessTokenProvider {
    private static final Logger logger = LoggerFactory.getLogger(AccessTokenProviderImplV3.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss Z")
            .withZone(ZoneId.systemDefault());

    private final RestTemplate restTemplate;
    private final CryptoSigner cryptoSigner;
    private final EsiaProperties esiaProperties;

    private final String endpointUrl;


    private final String scope;
    private final MultiValueMap<String, String> baseAccessTokenRequestBody;

    AccessTokenProviderImplV3(RestTemplate restTemplate, CryptoSigner cryptoSigner, EsiaProperties esiaProperties) {
        this.restTemplate = restTemplate;
        this.cryptoSigner = cryptoSigner;
        this.esiaProperties = esiaProperties;

        this.endpointUrl = esiaProperties.getEsiaBaseUrl() + "/aas/oauth2/v3/te";


        this.scope = esiaProperties.getScopesString();

        baseAccessTokenRequestBody = new LinkedMultiValueMap<>();
        baseAccessTokenRequestBody.add("client_id", esiaProperties.getClientId());
        baseAccessTokenRequestBody.add("scope", scope);
//        baseAccessTokenRequestBody.add("redirect_uri", urlEncode(esiaProperties.getReturnUrl()));
        baseAccessTokenRequestBody.add("client_certificate_hash", esiaProperties.getClientCertificateHash());
        baseAccessTokenRequestBody.add("grant_type", "authorization_code");
        baseAccessTokenRequestBody.add("token_type", "Bearer");
    }


    /**
     *  <client_secret> - подпись значений шести параметров в кодировке UTF-8:
     *  client_id;
     *  scope;
     *  timestamp;
     *  state;
     *  redirect_uri;
     *  code.
     * Порядок формирования <client_secret>:
     * 1. конкатенировать вышеуказанные параметры (порядок важен!).
     * 2. подписать полученную строку с использованием алгоритма подписания data hash с
     * использованием механизмов КриптоПРО CSP и сертификата информационной
     * системы;
     * 3. закодировать полученное значение в URL Safe Base64.
     *  <client_id> – идентификатор системы-клиента (мнемоника системы в ЕСИА указанная
     * прописными буквами);
     *  <scope> – область доступа, т.е. запрашиваемые права (то же самое значение, которое было
     * указано в запросе на получение авторизационного кода);
     *  <timestamp> – время запроса маркера в формате yyyy.MM.dd HH:mm:ss Z (например,
     * 2013.01.25 14:36:11 +0400), необходимое для фиксации начала временного промежутка,
     * в течение которого будет валиден запрос с данным идентификатором (<state>);
     *  <state> – набор случайных символов, имеющий вид 128-битного идентификатора запроса
     * (необходимо для защиты от перехвата), генерируется по стандарту UUID; этот набор
     * символов должен отличаться от того, который использовался при получении
     * авторизационного кода;
     *  <redirect_uri> – ссылка, по которой должен быть направлен пользователь после того, как
     * даст разрешение на доступ (то же самое значение, которое было указано в запросе
     * на получение авторизационного кода). Значение <redirect_uri> должно быть
     * предварительно указано в параметрах внешней ИС в ЕСИА - на стороне ЕСИА
     * выполняется верификация соответствия redirect_uri в запросе и в настройках системы;
     *  <client_certificate_hash> - параметр, содержащий хэш сертификата. Для вычисления значения
     * используется специализированная утилита;
     *  <code> – значение авторизационного кода, который был ранее получен от ЕСИА
     * и который необходимо обменять на маркер доступа;
     *  <grant_type> – принимает значение «authorization_code», если авторизационный код
     * обменивается на маркер доступа;
     *  <token_type> – тип запрашиваемого маркера, в настоящее время ЕСИА поддерживает
     * только значение «Bearer».
     */
    @Override
    public AccessTokenDto getAccessToken(String authorizationCode, String returnUrl) {
        try {
            String clientId = esiaProperties.getClientId();
            String state = generateState();
            String timestamp = generateTimestamp();

            String clientSecret = generateClientSecret(
                    ClientSecretParameters.builder().clientId(clientId).scope(scope).timestamp(timestamp).state(state).redirectUrl(returnUrl)
                            .authorizationCode(authorizationCode)
                            .build());

            MultiValueMap<String, String> postBody = new LinkedMultiValueMap<>(baseAccessTokenRequestBody);
            postBody.add("client_secret", clientSecret);
            postBody.add("timestamp", timestamp);
            postBody.add("state", state);
            postBody.add("code", authorizationCode);
            postBody.add("redirect_uri", urlEncode(returnUrl));


            logger.debug("fetching esia access token, post body parameters: {}", postBody);
            AccessTokenDto accessTokenDto = restTemplate.postForObject(endpointUrl, postBody, AccessTokenDto.class);

            logger.debug("accessTokenDto: {}", accessTokenDto);

            return accessTokenDto;
        } catch (HttpClientErrorException e) {
            throw new EsiaAccessException("Unable to get access token with API response status " + e.getRawStatusCode() + " and body: " + e.getResponseBodyAsString(), e);
        } catch (Exception e) {
            throw new EsiaAccessException("Unable to get access token for authorization code '" + authorizationCode + '\'', e);
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
        String[] toJoin = {p.clientId, p.scope, p.timestamp, p.state, p.redirectUrl, p.authorizationCode};
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
