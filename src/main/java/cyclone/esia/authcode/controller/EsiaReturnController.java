package cyclone.esia.authcode.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import cyclone.esia.authcode.EsiaProperties;
import cyclone.esia.authcode.dto.*;
import cyclone.esia.authcode.profile.Contacts;
import cyclone.esia.authcode.service.EsiaAuthUrlService;
import cyclone.esia.authcode.service.EsiaPublicKeyProvider;
import cyclone.esia.authcode.service.PersonDataCollectionType;
import io.jsonwebtoken.Header;
import io.jsonwebtoken.Jwt;
import io.jsonwebtoken.JwtParser;
import io.jsonwebtoken.Jwts;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequiredArgsConstructor
public class EsiaReturnController {
    private static final Logger logger = LoggerFactory.getLogger(EsiaReturnController.class);

    private final EsiaAuthUrlService esiaAuthUrlService;
    private final EsiaPublicKeyProvider esiaPublicKeyProvider;
    private final EsiaProperties esiaProperties;


    private JwtParser jwtParser;
    private JwtHeaderCheck[] authorizationCodeChecks;
    private JwtHeaderCheck[] accessTokenChecks;

    private final ObjectMapper objectMapper;

    private final RestTemplate restTemplate;

    @PostConstruct
    public void init() {
        jwtParser = Jwts.parser().setSigningKey(esiaPublicKeyProvider.getPublicKey())
                .requireIssuer(esiaProperties.getIssuer())
                .require("client_id", esiaProperties.getClientId());

        JwtHeaderCheck jwtTypeCheck = new JwtHeaderCheck("typ", "JWT");
        JwtHeaderCheck jwtAlgCheck = new JwtHeaderCheck("alg", "RS256");

        authorizationCodeChecks = new JwtHeaderCheck[]{jwtTypeCheck, jwtAlgCheck
                , new JwtHeaderCheck("sbt", "authorization_code")
        };

        accessTokenChecks = new JwtHeaderCheck[]{jwtTypeCheck, jwtAlgCheck
                , new JwtHeaderCheck("sbt", "access")
        };
    }


    @GetMapping(path = "/esia_return", produces = "text/plain")
    public String handleReturn(
            @RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription
    ) throws JsonProcessingException {

        StringJoiner joiner = new StringJoiner("\n\n");

        if (StringUtils.hasText(authorizationCode)) {
            joiner.add("сode=" + authorizationCode);
            tryJoinJWT(joiner, authorizationCode);
            parseAndCheckJwt(authorizationCode, "Authorization code", authorizationCodeChecks);
        }
        if (StringUtils.hasText(error)) {
            joiner.add("error=" + error);
        }
        if (StringUtils.hasText(errorDescription)) {
            joiner.add("error_description=" + errorDescription);
        }

        if (joiner.length() == 0) {
            joiner.add("No expected parameters received.");
        }


        if (StringUtils.hasText(authorizationCode)) {
            AccessTokenDto accessTokenDto = esiaAuthUrlService.getAccessToken(authorizationCode);
            joiner.add("accessTokenDto=" + accessTokenDto);
            tryJoinJWT(joiner, accessTokenDto.getAccessToken());
            parseAndCheckJwt(accessTokenDto.getAccessToken(), "Access token", accessTokenChecks);

            String jwtPayloadEncoded = accessTokenDto.getAccessToken().split("\\.")[1];
            byte[] jwtPayloadBytes = Base64Utils.decodeFromUrlSafeString(jwtPayloadEncoded);
            String jwtPayloadJsonString = new String(jwtPayloadBytes, StandardCharsets.UTF_8);
            long oid = objectMapper.readTree(jwtPayloadJsonString).get("urn:esia:sbj_id").asLong();
            logger.debug("oid: {}", oid);


            PersonalDataDto personalData = getPersonalDataDto(oid, accessTokenDto);
            joiner.add(personalData.toString());

            List<ContactDto> contactDtos = getCollection(oid, accessTokenDto, PersonDataCollectionType.CONTACTS, ContactDto.class);
            contactDtos.forEach(contactDto -> joiner.add(contactDto.toString()));




            Contacts contacts = mapToContacts(contactDtos);
            joiner.add(contacts.toString());

            List<AddressDto> addrs = getCollection(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES, AddressDto.class);
            addrs.forEach(addressDto -> joiner.add(addressDto.toString()));

            List<DocumentDto> documentDtos = getCollection(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS, DocumentDto.class);
            documentDtos.forEach(dto -> joiner.add(dto.toString()));

        }

        return joiner.toString();
    }

    private Contacts mapToContacts(List<ContactDto> contactDtos) {
        Contacts contacts = new Contacts();
        contactDtos.forEach(contactDto -> {
            String contactValue = contactDto.getValue();
            boolean verified = contactDto.getVerifiedStatus() == ContactDto.VerifiedStatus.VERIFIED;
            switch (contactDto.getType()) {
                case EML:
                    if (verified) contacts.addEmail(contactValue);
                    break;
                case MBT:
                    if (verified) contacts.addMobilePhone(contactValue);
                    break;
                case PHN:
                    contacts.addHomePhone(contactValue);
                    break;
                default:
                    logger.warn("Unknown contact type '{}'", contactDto.getType());
            }
        });
        return contacts;
    }

    private PersonalDataDto getPersonalDataDto(long oid, AccessTokenDto accessTokenDto) throws JsonProcessingException {
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", accessTokenDto.getTokenType() + " " + accessTokenDto.getAccessToken());
        HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);

        String url = "https://esia-portal1.test.gosuslugi.ru/rs/prns/" + oid;
        ResponseEntity<String> personalDataResponse = restTemplate.exchange(url, HttpMethod.GET, requestEntity, String.class);
        PersonalDataDto personalData = objectMapper.readValue(personalDataResponse.getBody(), PersonalDataDto.class);

        return personalData;
    }


    private <T> List<T> getCollection(long oid, AccessTokenDto accessTokenDto, PersonDataCollectionType collectionType, Class<T> resultClass) throws JsonProcessingException {
        List<T> results = new ArrayList<>();
        String collectionUrl = "https://esia-portal1.test.gosuslugi.ru/rs/prns/" + oid + "/" + collectionType.urlPart();
        HttpHeaders httpHeaders = new HttpHeaders();
        httpHeaders.add("Authorization", accessTokenDto.getTokenType() + " " + accessTokenDto.getAccessToken());
        HttpEntity<String> requestEntity = new HttpEntity<>(httpHeaders);
        ResponseEntity<String> collectionResponseEntity = restTemplate.exchange(collectionUrl, HttpMethod.GET, requestEntity, String.class);
        String collectionResponseString = collectionResponseEntity.getBody(); //  {"stateFacts":["hasSize"],"size":1,"eTag":"93E882A620BEDE1884695515724C772A43278794","elements":["https://esia-portal1.test.gosuslugi.ru/rs/prns/1000299654/ctts/14434265"]}
        logger.debug("collectionResponseString: {}", collectionResponseString);

        Iterator<JsonNode> collectionElements = objectMapper.readTree(collectionResponseString).get("elements").elements();
        while (collectionElements.hasNext()) {
            String elementUrl = collectionElements.next().asText();
            validateCollectionElementUrl(elementUrl);

            ResponseEntity<String> elementResponseEntity = restTemplate.exchange(elementUrl, HttpMethod.GET, requestEntity, String.class);
            String elementResponseString = elementResponseEntity.getBody();
            logger.debug(elementResponseString);

            T result = objectMapper.readValue(elementResponseString, resultClass);
            results.add(result);
        }
        return results;
    }


    private void validateCollectionElementUrl(String url) {
        if (!url.matches("https?:\\/\\/(.+?\\.)?gosuslugi\\.ru($|\\/.+)")) {
            throw new RuntimeException("Collection URL is not safe `" + url + '\'');
        }
    }


    private void tryJoinJWT(StringJoiner joiner, String possibleJWT) {
        String[] jwtParts = possibleJWT.split("\\.");
        if (jwtParts.length == 3) {
            joiner.add("try base64decode jwt");
            joiner.add(new String(Base64Utils.decodeFromUrlSafeString(jwtParts[0]), StandardCharsets.UTF_8));
            joiner.add(new String(Base64Utils.decodeFromUrlSafeString(jwtParts[1]), StandardCharsets.UTF_8));
        }
    }

    private Jwt parseAndCheckJwt(String jwtString, String tokenTypeTitle, JwtHeaderCheck[] checks) {
        Jwt jwt = jwtParser.parse(jwtString);

        Header header = jwt.getHeader();
//        Claims claims = (Claims) jwt.getBody();

        performJwtHeaderChecks(header, tokenTypeTitle, checks);

        return jwt;
    }

    private void performJwtHeaderChecks(Header header, String title, JwtHeaderCheck[] checks) {
        List<JwtHeaderCheck> failedChecks = Arrays.stream(checks)
                .filter(check -> !check.getExpectedValue().equals(check.apply(header)))
                .collect(Collectors.toList());

        if (!failedChecks.isEmpty()) {
            String failedChecksMessage = failedChecks.stream()
                    .map(check -> "\"" + check.getKey() + "\" value expected: \"" + check.getExpectedValue() + "\", actual: \"" + check.getActualValue() + '"')
                    .collect(Collectors.joining("; "));
            throw new RuntimeException(title + " checks failed: " + failedChecksMessage);
        }
    }

    @RequiredArgsConstructor
    @Getter
    static class JwtHeaderCheck {
        private final String key;
        private final Object expectedValue;
        private Object actualValue;

        public Object apply(Header header) {
            if (actualValue == null) {
                actualValue = header.get(key);
            }
            return actualValue;
        }
    }

}
