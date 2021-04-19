package cyclone.esia.authcode.controller;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import cyclone.esia.authcode.EsiaProperties;
import cyclone.esia.authcode.dto.*;
import cyclone.esia.authcode.profile.Contacts;
import cyclone.esia.authcode.profile.JsonNodeProfile;
import cyclone.esia.authcode.service.EsiaAccessService;
import cyclone.esia.authcode.service.EsiaPublicKeyProvider;
import cyclone.esia.authcode.service.PersonDataCollectionType;
import cyclone.esia.authcode.service.PersonalDataServiceImpl;
import io.jsonwebtoken.*;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import javax.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Objects;
import java.util.StringJoiner;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RestController
@RequiredArgsConstructor
public class EsiaReturnController {
    private static final Logger logger = LoggerFactory.getLogger(EsiaReturnController.class);

    private final EsiaAccessService esiaAuthUrlService;
    private final EsiaPublicKeyProvider esiaPublicKeyProvider;
    private final EsiaProperties esiaProperties;
    private final PersonalDataServiceImpl personalDataService;

    private JwtParser jwtParser;

    private final XmlMapper xmlMapper;

    private final JwtHeaderCheck[] accessTokenChecks = new JwtHeaderCheck[]{
            new JwtHeaderCheck("typ", "JWT")
            , new JwtHeaderCheck("alg", "RS256")
            , new JwtHeaderCheck("sbt", "access")
    };

    @PostConstruct
    public void init() {
        jwtParser = Jwts.parser().setSigningKey(esiaPublicKeyProvider.getPublicKey())
                .requireIssuer(esiaProperties.getIssuer())
                .require("client_id", esiaProperties.getClientId())
                .setAllowedClockSkewSeconds(5);
    }


    @GetMapping(path = "/login/oauth2/code/esia_display", produces = "text/plain")
    public String handleReturn(
            @RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription
    ) throws JsonProcessingException {

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
            AccessTokenDto accessTokenDto = esiaAuthUrlService.getAccessToken(authorizationCode);
            joiner.add("accessTokenDto=" + accessTokenDto);
            tryJoinJWT(joiner, accessTokenDto.getAccessToken());

            Jwt<Header, Claims> accessTokenJwt = parseAndCheckAccessTokenJwt(accessTokenDto.getAccessToken());

            long oid = accessTokenJwt.getBody().get("urn:esia:sbj_id", Long.class);
            logger.debug("oid: {}", oid);


            PersonalDataDto personalData = personalDataService.getPersonalDataDto(oid, accessTokenDto);
            joiner.add(personalData.toString());

//            List<ContactDto> contactDtos = getCollection(oid, accessTokenDto, PersonDataCollectionType.CONTACTS, ContactDto.class);
            List<ContactDto> contactDtos = personalDataService.getCollectionEmbedded(oid, accessTokenDto, PersonDataCollectionType.CONTACTS, ContactDto.class);
            contactDtos.forEach(contactDto -> joiner.add(contactDto.toString()));



            Contacts contacts = personalDataService.mapToContacts(contactDtos);
            joiner.add(contacts.toString());

//            List<AddressDto> addrs = personalDataService.getCollection(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES, AddressDto.class);
            List<AddressDto> addrs = personalDataService.getCollectionEmbedded(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES, AddressDto.class);
            addrs.forEach(addressDto -> joiner.add(addressDto.toString()));

//            List<DocumentDto> documentDtos = getCollection(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS, DocumentDto.class);
            List<DocumentDto> documentDtos = personalDataService.getCollectionEmbedded(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS, DocumentDto.class);
            documentDtos.forEach(dto -> joiner.add(dto.toString()));

//            Profile profile = new Profile(personalData, addrs, contactDtos, documentDtos);
//            String profileXml = xmlMapper.writeValueAsString(profile);
//            logger.debug("profileXml: {}", profileXml);


            JsonNode personalDataJsonNode = personalDataService.getPersonalDataAsJsonNode(oid, accessTokenDto);
            List<JsonNode> contactsJsonNode = personalDataService.getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.CONTACTS);
            List<JsonNode> addressesJsonNode = personalDataService.getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.ADDRESSES);
            List<JsonNode> documentsJsonNode = personalDataService.getCollectionEmbeddedAsJsonNodes(oid, accessTokenDto, PersonDataCollectionType.DOCUMENTS);

            JsonNodeProfile jsonNodeProfile = new JsonNodeProfile(personalDataJsonNode, addressesJsonNode
                    , contactsJsonNode, documentsJsonNode);

//            String jsonNodexml = xmlMapper.writeValueAsString(jsonNodeProfile);
            String jsonNodexml = xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(jsonNodeProfile);

            logger.debug("jsonNodexml: {}", jsonNodexml);

            joiner.add("xml:");
            joiner.add(jsonNodexml);
        }

        return joiner.toString();
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

        List<JwtHeaderCheckResult> failedChecks = Stream.of(accessTokenChecks)
                .map(check -> new JwtHeaderCheckResult(check, header.get(check.getKey())))
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
        private final JwtHeaderCheck jwtHeaderCheck;
        private final Object actualValue;

        boolean isPassed() {
            return Objects.equals(jwtHeaderCheck.getExpectedValue(), actualValue);
        }
    }
}
