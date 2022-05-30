package ru.unisuite.identity.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.dto.AccessTokenDto;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.Base64;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
class EsiaAccessServiceImpl implements EsiaAccessService {
    private static final Logger logger = LoggerFactory.getLogger(EsiaAccessServiceImpl.class);

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss Z")
            .withZone(ZoneId.systemDefault());

    private final RestTemplate restTemplate;
    private final CryptoSigner cryptoSigner;
    private final EsiaProperties esiaProperties;


    private final String scope;
    private final UriComponentsBuilder baseAuthCodeUriBuilder;
    private final MultiValueMap<String, String> baseAccessTokenRequestBody;

    EsiaAccessServiceImpl(RestTemplate restTemplate, CryptoSigner cryptoSigner, EsiaProperties esiaProperties) {
        this.restTemplate = restTemplate;
        this.cryptoSigner = cryptoSigner;
        this.esiaProperties = esiaProperties;


        Scope[] scopes = {Scope.FULLNAME, Scope.GENDER, Scope.BIRTHDATE
                , Scope.BIRTHPLACE, Scope.BIRTH_CERT_DOC, Scope.CONTACTS, Scope.INN, Scope.SNILS, Scope.RESIDENCE_DOC
                , Scope.TEMPORARY_RESIDENCE_DOC
                , Scope.ID_DOC, Scope.TEMPORARY_RESIDENCE_DOC
        };
        scope = Stream.of(scopes).map(streamScope -> streamScope.toString().toLowerCase()).collect(Collectors.joining(" "));


        baseAuthCodeUriBuilder = UriComponentsBuilder.fromHttpUrl(esiaProperties.getAuthCodeUrl())
                .queryParam("client_id", esiaProperties.getClientId())
                .queryParam("scope", scope)
                .queryParam("response_type", "code")
                .queryParam("access_type", "offline"); // "offline" or "online"


        baseAccessTokenRequestBody = new LinkedMultiValueMap<>();
        baseAccessTokenRequestBody.add("client_id", esiaProperties.getClientId());
        baseAccessTokenRequestBody.add("grant_type", "authorization_code");
        baseAccessTokenRequestBody.add("scope", scope);
        baseAccessTokenRequestBody.add("token_type", "Bearer");
        baseAccessTokenRequestBody.add("redirect_uri", urlEncode(esiaProperties.getReturnUrl()));
    }


    @Override
    public String generateAuthCodeUrl() {
        try {
            String timestamp = generateTimestamp();
            String clientId = esiaProperties.getClientId();
            String state = generateState();
            String clientSecret = generateClientSecret(scope, timestamp, clientId, state);

            UriComponentsBuilder authCodeUriBuilder = baseAuthCodeUriBuilder.cloneBuilder()
                    .queryParam("client_secret", clientSecret)
                    .queryParam("state", state);

            String url = authCodeUriBuilder.toUriString();
            url += "&timestamp=" + urlEncode(timestamp);
            url += "&redirect_uri=" + urlEncode(esiaProperties.getReturnUrl());

            logger.debug("authentication code url: {}", url);

            return url;
        } catch (Exception e) {
            throw new EsiaAccessException("Unable to generate access token url", e);
        }
    }


    /**
     *  <client_id> – идентификатор системы-клиента (мнемоника системы в ЕСИА указанная прописными буквами);
     *  <code> – значение авторизационного кода, который был ранее получен от ЕСИА и который необходимо обменять на
     * маркер доступа;
     *  <grant_type> – принимает значение “authorization_code”, если авторизационный код обменивается на маркер
     * доступа;
     *  <client_secret> – подпись запроса в формате PKCS#7 detached signature в кодировке UTF8 от значений четырех
     * параметров HTTP–запроса: scope, timestamp, clientId, state (без разделителей). <client_secret> должен быть
     * закодирован в формате base64 url safe. Используемый для проверки подписи сертификат должен быть предварительно
     * зарегистрирован в ЕСИА и привязан к учетной записи системы-клиента в ЕСИА. ЕСИА поддерживает сертификаты в
     * формате X.509. ЕСИА поддерживает алгоритм формирования электронной подписи ГОСТ Р 34.10-2012 и алгоритм
     * криптографического хэширования ГОСТ Р 34.11-2012.
     *  <state> – набор случайных символов, имеющий вид 128-битного идентификатора запроса (необходимо для защиты от
     * перехвата), генерируется по стандарту UUID; этот набор символов должен отличаться от того, который
     * использовался при получении авторизационного кода;
     *  <redirect_uri> – ссылка, по которой должен быть направлен пользователь после того, как даст разрешение на
     * доступ (то же самое значение, которое было указано в запросе на получение авторизационного кода);
     *  <scope> – область доступа, т.е. запрашиваемые права (то же самое значение, которое было указано в запросе на
     * получение авторизационного кода);
     *  <timestamp> – время запроса маркера в формате yyyy.MM.dd HH:mm:ss Z (например, 2013.01.25 14:36:11 +0400),
     * необходимое для фиксации начала временного промежутка, в течение которого будет валиден запрос с данным
     * идентификатором (<state>);
     *  <token_type> – тип запрашиваемого маркера, в настоящее время ЕСИА поддерживает только значение “Bearer
     */
    @Override
    public AccessTokenDto getAccessToken(String authenticationCode) {
        try {
            String clientId = esiaProperties.getClientId();
            String state = generateState();
            String timestamp = generateTimestamp();
            String clientSecret = generateClientSecret(scope, timestamp, clientId, state);

            MultiValueMap<String, String> postBody = new LinkedMultiValueMap<>(baseAccessTokenRequestBody);
            postBody.add("code", authenticationCode);
            postBody.add("client_secret", clientSecret);
            postBody.add("state", state);
            postBody.add("timestamp", timestamp);

            logger.debug("fetching esia access token, post body parameters: {}", postBody);
            AccessTokenDto accessTokenDto = restTemplate.postForObject(esiaProperties.getAccessTokenUrl(), postBody, AccessTokenDto.class);
//            logger.debug("response: {}", response);

            logger.debug("accessTokenDto: {}", accessTokenDto);

            return accessTokenDto;
        } catch (Exception e) {
            throw new EsiaAccessException("Unable to get access token for authorization code '" + authenticationCode + '\'', e);
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

    private String generateClientSecret(String scope, String timestamp, String clientId, String state) {
        String clientSecretUnsigned = String.join("", scope, timestamp, clientId, state);
        logger.debug("clientSecretUnsigned: {}", clientSecretUnsigned);

        byte[] signedClientSecretBytes = cryptoSigner.signPkcs7Detached(clientSecretUnsigned);
        return Base64.getUrlEncoder().encodeToString(signedClientSecretBytes);
    }

}
