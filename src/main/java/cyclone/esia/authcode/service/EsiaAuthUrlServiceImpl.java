package cyclone.esia.authcode.service;

import cyclone.esia.authcode.EsiaProperties;
import cyclone.esia.authcode.dto.AccessTokenDto;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

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
@RequiredArgsConstructor
class EsiaAuthUrlServiceImpl implements EsiaAuthUrlService {
    private static final Logger logger = LoggerFactory.getLogger(EsiaAuthUrlServiceImpl.class);

    private final CryptoSigner cryptoSigner;
    private final EsiaProperties esiaProperties;

    private final DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm:ss Z")
            .withZone(ZoneId.systemDefault());

    private static final String accessType = "offline"; // "online";
    private static final Scope[] scopes = {Scope.FULLNAME, Scope.GENDER, Scope.BIRTHDATE
            , Scope.BIRTHPLACE, Scope.BIRTH_CERT_DOC, Scope.CONTACTS, Scope.INN, Scope.SNILS, Scope.RESIDENCE_DOC
            , Scope.TEMPORARY_RESIDENCE_DOC
            , Scope.ID_DOC, Scope.TEMPORARY_RESIDENCE_DOC
//            , Scope.FOREIGN_PASSPORT_DOC // -> ESIA-007019: OAuthErrorEnum.noGrants
    };
//    private final String scope = "fullname foreign_passport_doc"; // -> ESIA-007019: OAuthErrorEnum.noGrants

    private final String scope = Stream.of(scopes).map(streamScope -> streamScope.toString().toLowerCase()).collect(Collectors.joining(" "));

    private static final String responseType = "code";

    private static final String accessGrantType = "authorization_code";
    private static final String accessTokenType = "Bearer";


    @Override
    public String generateAuthCodeUrl() {
        try {
            String timestamp = generateTimestamp();
            String clientId = esiaProperties.getClientId();
            String state = generateState();
            String clientSecret = generateClientSecret(scope, timestamp, clientId, state);

            UriComponentsBuilder accessTokenRequestBuilder = UriComponentsBuilder.fromHttpUrl(esiaProperties.getAuthCodeUrl())
                    .queryParam("client_id", clientId)
                    .queryParam("client_secret", clientSecret)
                    .queryParam("scope", scope)
                    .queryParam("response_type", responseType)
                    .queryParam("state", state)
                    .queryParam("access_type", accessType);

            String url = accessTokenRequestBuilder.toUriString();
            url += "&timestamp=" + urlEncode(timestamp);
            url += "&redirect_uri=" + urlEncode(esiaProperties.getReturnUrl());

            logger.debug("authentication code url: {}", url);

            return url;
        } catch (Exception e) {
            throw new EsiaAuthUrlServiceException("Unable to generate access token url", e);
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
     * закодирован в формате base64 url safe. Используемый для проверки подписи сертификат должен быть
     * предварительно зарегистрирован в ЕСИА и привязан к учетной записи системы-клиента в ЕСИА. ЕСИА поддерживает
     * сертификаты в формате X.509. ЕСИА поддерживает алгоритм формирования электронной подписи ГОСТ Р 34.10-2012 и
     * алгоритм криптографического хэширования ГОСТ Р 34.11-2012.
     *  <state> – набор случайных символов, имеющий вид 128-битного идентификатора запроса (необходимо для защиты от
     * перехвата), генерируется по стандарту UUID; этот набор символов должен отличаться от того, который
     * использовался при получении авторизационного кода;
     *  <redirect_uri> – ссылка, по которой должен быть направлен пользователь после того, как даст разрешение на
     * доступ (то же самое значение, которое было указано в запросе на получение авторизационного кода);
     *  <scope> – область доступа, т.е. запрашиваемые права (то же самое значение, которое было указано в запросе на
     * получение авторизационного кода);
     *  <timestamp> – время запроса маркера в формате yyyy.MM.dd HH:mm:ss Z (например,
     * 2013.01.25 14:36:11 +0400), необходимое для фиксации начала временного промежутка, в
     * течение которого будет валиден запрос с данным идентификатором (<state>);
     *  <token_type> – тип запрашиваемого маркера, в настоящее время ЕСИА поддерживает
     * только значение “Bearer
     */
    @Override
    public AccessTokenDto getAccessToken(String authenticationCode) {
        try {
            String clientId = esiaProperties.getClientId();
            String state = generateState();
            String timestamp = generateTimestamp();
            String clientSecret = generateClientSecret(scope, timestamp, clientId, state);

            String redirectUrlEncoded = urlEncode(esiaProperties.getReturnUrl());

            MultiValueMap<String, String> postBody = new LinkedMultiValueMap<>();
            postBody.add("client_id", clientId);
            postBody.add("code", authenticationCode);
            postBody.add("grant_type", accessGrantType);
            postBody.add("client_secret", clientSecret);
            postBody.add("state", state);
            postBody.add("scope", scope);
            postBody.add("token_type", accessTokenType);
            postBody.add("timestamp", timestamp);
            postBody.add("redirect_uri", redirectUrlEncoded);

            logger.debug("access token post body parameters: {}", postBody);
            AccessTokenDto accessTokenDto = new RestTemplate().postForObject(esiaProperties.getAccessTokenUrl(), postBody, AccessTokenDto.class);
//            String response = new RestTemplate().postForObject(esiaProperties.getAccessTokenUrl(), httpEntity, String.class);
//            logger.debug("response: {}", response);

            logger.debug("accessTokenDto: {}", accessTokenDto);

            return accessTokenDto;
        } catch (Exception e) {
            throw new EsiaAuthUrlServiceException("Unable to get access token for authorization code '" + authenticationCode + '\'', e);
        }
    }


    private String urlEncode(String string) throws UnsupportedEncodingException {
        return URLEncoder.encode(string, StandardCharsets.UTF_8.name());
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
