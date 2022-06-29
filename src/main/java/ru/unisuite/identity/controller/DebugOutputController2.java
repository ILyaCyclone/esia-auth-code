package ru.unisuite.identity.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.jsonwebtoken.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.MediaType;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.cabinet.CabinetAuthorizationDto;
import ru.unisuite.identity.cabinet.CabinetProfileService;
import ru.unisuite.identity.dto.*;
import ru.unisuite.identity.oauth2.Oauth2Flow;
import ru.unisuite.identity.oauth2.Scope;
import ru.unisuite.identity.profile.Contacts;
import ru.unisuite.identity.profile.ProfileJsonNode;
import ru.unisuite.identity.service.EsiaAccessException;
import ru.unisuite.identity.service.EsiaPublicKeyProvider;
import ru.unisuite.identity.service.PersonDataCollectionType;
import ru.unisuite.identity.service.PersonalDataServiceImpl;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequestMapping(produces = MediaType.TEXT_PLAIN_VALUE)
@RequiredArgsConstructor
public class DebugOutputController2 {
    private static final Logger logger = LoggerFactory.getLogger(DebugOutputController2.class);

    //    private final String baseUrl = "http://localhost:59185/esia_debug";
    private final String baseUrl = "http://wl3n3.miit.ru:7003/esia_debug";

    private final EsiaPublicKeyProvider esiaPublicKeyProvider;
    private final EsiaProperties esiaProperties;
    private final PersonalDataServiceImpl personalDataService;
    private final CabinetProfileService cabinetProfileService;

    private final Oauth2Flow oauth2Flow;
    private final Set<Scope> scopes;

    private JwtParser jwtParser;

    private final XmlMapper xmlMapper;

    private final EsiaDebugOutputReturnController.JwtHeaderCheck[] accessTokenChecks = new EsiaDebugOutputReturnController.JwtHeaderCheck[]{
            new EsiaDebugOutputReturnController.JwtHeaderCheck("typ", "JWT")
            , new EsiaDebugOutputReturnController.JwtHeaderCheck("alg", "RS256")
            , new EsiaDebugOutputReturnController.JwtHeaderCheck("sbt", "access")
    };

    @PostConstruct
    public void init() {
        jwtParser = Jwts.parser().setSigningKey(esiaPublicKeyProvider.getPublicKey())
                .requireIssuer(esiaProperties.getIssuer())
                .require("client_id", esiaProperties.getClientId())
                .setAllowedClockSkewSeconds(5);
    }

    // --------------------------------

    @GetMapping("/d2/authcode")
    public RedirectView authorizationCode() {
        return new RedirectView(oauth2Flow.generateAuthorizationCodeURL(baseUrl + "/d2/return/authcode"));
    }

    @GetMapping("/d2/return/authcode")
    public String authorizationCodeReturn(@RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription) {
        StringJoiner joiner = new StringJoiner("\n\n");

        if (StringUtils.hasText(authorizationCode)) {
            joiner.add("сode=" + authorizationCode);
            tryJoinJWT(joiner, authorizationCode);
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
        return joiner.toString();
    }

    // --------------------------------

    String returnUrl2 = baseUrl + "/d2/return/accesstoken";

    @GetMapping("/d2/accesstoken")
    public RedirectView accessToken() {
        return new RedirectView(oauth2Flow.generateAuthorizationCodeURL(returnUrl2));
    }

    @GetMapping("/d2/return/accesstoken")
    public String accessTokenReturn(@RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription) {
        StringJoiner joiner = new StringJoiner("\n\n");

        if (StringUtils.hasText(authorizationCode)) {
            joiner.add("сode=" + authorizationCode);
            tryJoinJWT(joiner, authorizationCode);
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
            AccessTokenDto accessTokenDto = oauth2Flow.getAccessToken(authorizationCode);

            joiner.add("\n========================================\n");
            joiner.add("accessTokenDto=" + accessTokenDto);
            tryJoinJWT(joiner, accessTokenDto.getAccessToken());

            Jwt<Header, Claims> accessTokenJwt = parseAndCheckAccessTokenJwt(accessTokenDto.getAccessToken());

            long oid = accessTokenJwt.getBody().get("urn:esia:sbj_id", Long.class);
            joiner.add("oid=" + oid);
        }

        return joiner.toString();
    }

    // --------------------------------

    String returnUrl3 = baseUrl + "/d2/return/personaldata";

    @GetMapping("/d2/personaldata")
    public RedirectView personalData() {
        return new RedirectView(oauth2Flow.generateAuthorizationCodeURL(returnUrl3));
    }

    @GetMapping("/d2/return/personaldata")
    public String personalDataReturn(@RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription) throws JsonProcessingException {
        StringJoiner joiner = new StringJoiner("\n\n");

        if (StringUtils.hasText(authorizationCode)) {
            joiner.add("сode=" + authorizationCode);
            tryJoinJWT(joiner, authorizationCode);
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
            AccessTokenDto accessTokenDto = oauth2Flow.getAccessToken(authorizationCode);

            joiner.add("\n========================================\n");
            joiner.add("accessTokenDto=" + accessTokenDto);
            tryJoinJWT(joiner, accessTokenDto.getAccessToken());

            Jwt<Header, Claims> accessTokenJwt = parseAndCheckAccessTokenJwt(accessTokenDto.getAccessToken());

            long oid = accessTokenJwt.getBody().get("urn:esia:sbj_id", Long.class);
            joiner.add("oid=" + oid);

            PersonalDataDto personalData = personalDataService.getPersonalDataDto(oid, accessTokenDto);

            joiner.add("\n========================================\n");
            joiner.add(personalData.toString());

//            List<ContactDto> contactDtos = getCollection(oid, accessTokenDto, PersonDataCollectionType.CONTACTS, ContactDto.class);
            List<ContactDto> contactDtos = personalDataService.getCollectionEmbedded(oid, accessTokenDto, PersonDataCollectionType.CONTACTS, ContactDto.class);
            contactDtos.forEach(contactDto -> joiner.add(contactDto.toString()));



            Contacts contacts = personalDataService.mapToContacts(contactDtos);
            joiner.add(contacts.toString());

            if (scopes.contains(Scope.ADDRESSES)) {
                List<AddressDto> addrs = personalDataService.getCollectionEmbedded(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES, AddressDto.class);
                addrs.forEach(addressDto -> joiner.add(addressDto.toString()));
            } else {
                logger.warn("Scopes do not contain '{}', so '{}' personal data collection won't be fetched",
                        Scope.ADDRESSES, PersonDataCollectionType.ADDRESSES);
            }

            List<DocumentDto> documentDtos = personalDataService.getCollectionEmbedded(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS, DocumentDto.class);
            documentDtos.forEach(dto -> joiner.add(dto.toString()));


            JsonNode personalDataJsonNode = personalDataService.getPersonalDataAsJsonNode(oid, accessTokenDto);
            List<JsonNode> contactsJsonNode = personalDataService.getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.CONTACTS);
            List<JsonNode> documentsJsonNode = personalDataService.getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS);
            List<JsonNode> addressesJsonNode;
            if (scopes.contains(Scope.ADDRESSES)) {
                addressesJsonNode = personalDataService.getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES);
            } else {
                addressesJsonNode = Collections.emptyList();
                logger.warn("Scopes do not contain '{}', so '{}' personal data collection won't be fetched",
                        Scope.ADDRESSES, PersonDataCollectionType.ADDRESSES);
            }

            ProfileJsonNode jsonNodeProfile = new ProfileJsonNode(personalDataJsonNode, addressesJsonNode
                    , contactsJsonNode, documentsJsonNode);

            String profileXml = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNodeProfile);

            joiner.add("xml:");
            joiner.add(profileXml);
        }

        return joiner.toString();
    }

    // --------------------------------

    String returnUrl4 = baseUrl + "/d2/return/cabinet";

    @GetMapping("/d2/cabinet")
    public RedirectView cabinet() {
        return new RedirectView(oauth2Flow.generateAuthorizationCodeURL(returnUrl4));
    }

    @GetMapping("/d2/return/cabinet")
    public RedirectView cabinetReturn(
            @RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new EsiaAccessException("Authorization code not received. Error: '" + error
                    + "'. Description: '" + errorDescription + "'.");
        }

        CabinetAuthorizationDto cabinetAuthorizationCodeDto = cabinetProfileService.getCabinetAuthorizationCode(authorizationCode);

        String cabinetUrl = UriComponentsBuilder.fromHttpUrl(esiaProperties.getCabinetRedirectUrl())
                .queryParam("token", cabinetAuthorizationCodeDto.getAuthorizationCode())
                .queryParam("userId", cabinetAuthorizationCodeDto.getEsiaOid())
                .queryParam("provider", "esia").toUriString();

        return new RedirectView(cabinetUrl);
    }



    private void tryJoinJWT(StringJoiner joiner, String possibleJWT) {
        String[] jwtParts = possibleJWT.split("\\.");
        if (jwtParts.length == 3) {
            joiner.add("try base64decode jwt");
            joiner.add(new String(Base64Utils.decodeFromUrlSafeString(jwtParts[0]), StandardCharsets.UTF_8));
            joiner.add(new String(Base64Utils.decodeFromUrlSafeString(jwtParts[1]), StandardCharsets.UTF_8));
        }
    }

    private Jwt<Header, Claims> parseAndCheckAccessTokenJwt(String accessToken) {
        Jwt<Header, Claims> jwt = jwtParser.parse(accessToken);
        Header header = jwt.getHeader();

        List<EsiaDebugOutputReturnController.JwtHeaderCheckResult> failedChecks = Stream.of(accessTokenChecks)
                .map(check -> new EsiaDebugOutputReturnController.JwtHeaderCheckResult(check, header.get(check.getKey())))
                .filter(performedCheck -> !performedCheck.isPassed())
                .collect(Collectors.toList());

        if (!failedChecks.isEmpty()) {
            String failedChecksMessage = failedChecks.stream()
                    .map(check -> "Expected '" + check.getJwtHeaderCheck().getKey() + "' header to be: '" + check.getJwtHeaderCheck().getExpectedValue()
                            + "', but was: '" + check.getActualValue() + '\'')
                    .collect(Collectors.joining("; "));
            throw new JwtException("Access token check failed: " + failedChecksMessage);
        }

        return jwt;
    }



    @Data
    static class JwtHeaderCheck {
        private final String key;
        private final Object expectedValue;
    }

    @Data
    static class JwtHeaderCheckResult {
        private final EsiaDebugOutputReturnController.JwtHeaderCheck jwtHeaderCheck;
        private final Object actualValue;

        boolean isPassed() {
            return Objects.equals(jwtHeaderCheck.getExpectedValue(), actualValue);
        }
    }



}
