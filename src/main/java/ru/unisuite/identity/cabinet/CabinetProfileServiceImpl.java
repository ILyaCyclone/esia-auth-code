package ru.unisuite.identity.cabinet;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import io.jsonwebtoken.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.config.ClientProperties;
import ru.unisuite.identity.dto.AccessTokenDto;
import ru.unisuite.identity.dto.PersonalDataDto;
import ru.unisuite.identity.oauth2.Oauth2Flow;
import ru.unisuite.identity.profile.ProfileJsonNode;
import ru.unisuite.identity.service.EsiaPublicKeyProvider;
import ru.unisuite.identity.service.PersonalDataService;

import javax.annotation.PostConstruct;
import java.sql.Types;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CabinetProfileServiceImpl implements CabinetProfileService {
    private static final Logger logger = LoggerFactory.getLogger(CabinetProfileServiceImpl.class);

    private static final String CABINET_CLIENT_NAME = "cabinet";

    private final Oauth2Flow oauth2Flow;
    private final PersonalDataService personalDataService;
    private final EsiaPublicKeyProvider esiaPublicKeyProvider;
    private final EsiaProperties esiaProperties;

    private final String returnUrl;

    private final NamedParameterJdbcTemplate jdbcTemplate;
    private final XmlMapper xmlMapper;

    private JwtParser jwtParser;

    private final JwtHeaderCheck[] accessTokenChecks = new JwtHeaderCheck[]{
            new JwtHeaderCheck("typ", "JWT")
            , new JwtHeaderCheck("alg", "RS256") //TODO get algorithm from properties
            , new JwtHeaderCheck("sbt", "access")
    };

    @PostConstruct
    public void init() {
        jwtParser = Jwts.parserBuilder().setSigningKey(esiaPublicKeyProvider.getPublicKey())
                .requireIssuer(esiaProperties.getIssuer())
                .require("client_id", esiaProperties.getClientId())
                .setAllowedClockSkewSeconds(esiaProperties.getJwtAllowedClockSkewSeconds())
                .build();
    }

    //    private final SimpleJdbcCall getCabinetAuthorizationCodeJdbcCall;
    private static final String CABINET_AUTH_CODE_SQL = "select wpms2_auth_wp.create_access_token(" +
            "  A_provider_user_id      => :A_provider_user_id " +
            ", A_auth_provider         => :A_auth_provider " +
            ", A_app_logical_name      => :A_app_logical_name " +
            ", A_provider_access_token => :A_provider_access_token " +
            ") from dual";
    private final SimpleJdbcCall registerProfileJdbcCall;


    public CabinetProfileServiceImpl(Oauth2Flow oauth2Flow, PersonalDataService personalDataService,
                                     EsiaPublicKeyProvider esiaPublicKeyProvider, EsiaProperties esiaProperties, ClientProperties clientProperties,
                                     NamedParameterJdbcTemplate jdbcTemplate, XmlMapper xmlMapper) {

        this.oauth2Flow = oauth2Flow;
        this.personalDataService = personalDataService;
        this.esiaPublicKeyProvider = esiaPublicKeyProvider;
        this.esiaProperties = esiaProperties;
        this.jdbcTemplate = jdbcTemplate;
        this.xmlMapper = xmlMapper;

        this.returnUrl = clientProperties.getRegistration().get(CABINET_CLIENT_NAME).getRedirectUri();

        this.registerProfileJdbcCall = new SimpleJdbcCall(jdbcTemplate.getJdbcTemplate())
                .withoutProcedureColumnMetaDataAccess()
                .withSchemaName("pilot")
                .withCatalogName("p_esia_prn")
                .withProcedureName("set_request")
//                .withNamedBinding()
                .declareParameters(
                        new SqlParameter("A_esia_oid", Types.BIGINT)
                        , new SqlParameter("A_data", Types.CLOB)
                        , new SqlParameter("A_log_request", Types.CLOB)
                );

    }

    @Override
    public CabinetAuthorizationDto getCabinetAuthorizationCode(String providerAlias, String clientAlias, String providerAuthorizationCode) {
        AccessTokenDto esiaAccessTokenDto = oauth2Flow.getAccessToken(providerAuthorizationCode, returnUrl);

        Jwt<Header, Claims> accessTokenJwt = parseAndCheckAccessTokenJwt(esiaAccessTokenDto.getAccessToken());

        long oid = extractOidFromAccessTokenJwt(accessTokenJwt);

        boolean profileRegistrationRequired = checkProfileRegistrationRequired(providerAlias, oid);

        if (profileRegistrationRequired) {
            registerProfile(oid, esiaAccessTokenDto, "");
        }

        String cabinetAuthorizationCode = getCabinetAuthorizationCode(providerAlias, clientAlias, oid, esiaAccessTokenDto);

        return new CabinetAuthorizationDto(oid, cabinetAuthorizationCode);
    }


    @Override
    public String fetchProfileXml(long oid, AccessTokenDto esiaAccessTokenDto) {
        ProfileJsonNode profileJsonNode = personalDataService.getProfileJsonNode(oid, esiaAccessTokenDto);
        return profileJsonNodeToXml(oid, profileJsonNode);
    }

    private void registerProfile(long oid, AccessTokenDto esiaAccessTokenDto, String technicalLog) {
        try {
            ProfileJsonNode profileJsonNode = personalDataService.getProfileJsonNode(oid, esiaAccessTokenDto);
            PersonalDataDto personalData = personalDataService.extractPersonalData(profileJsonNode);

            if (!personalData.isTrusted()) {
                throw new ProfileIsNotTrustedException("Profile is not trusted {oid=" + oid + '}', oid, personalData);
            }

            String profileXml = fetchProfileXml(oid, esiaAccessTokenDto);

            SqlParameterSource params = new MapSqlParameterSource()
                    .addValue("A_esia_oid", oid)
                    .addValue("A_data", profileXml)
                    .addValue("A_log_request", technicalLog);
            registerProfileJdbcCall.execute(params);
        } catch (ProfileIsNotTrustedException e) {
            throw e;
        } catch (Exception e) {
            throw new CabinetProfileServiceException("Could not registerProfile {oid=" + oid + '}', e);
        }
    }


    private Long extractOidFromAccessTokenJwt(Jwt<Header, Claims> accessTokenJwt) {
        //TODO this assumes that authprovider is esia, won't work for different
        try {
            return accessTokenJwt.getBody().get("urn:esia:sbj_id", Long.class);
        } catch (Exception e) {
            throw new CabinetProfileServiceException("Could not extract oid from access token {access token body='" +
                    accessTokenJwt.getBody() + '}', e);
        }
    }


    private String profileJsonNodeToXml(long oid, ProfileJsonNode profileJsonNode) {
        ProfileJsonNodeXmlMapping profileJsonNodeXmlMapping = new ProfileJsonNodeXmlMapping(profileJsonNode);
        try {
            return xmlMapper.writerWithDefaultPrettyPrinter().writeValueAsString(profileJsonNodeXmlMapping);
        } catch (JsonProcessingException e) {
            throw new CabinetProfileServiceException("Could not map ProfileJsonNode to xml {oid=" + oid + "}", e);
        }
    }

    private String getCabinetAuthorizationCode(String providerAlias, String clientAlias, long oid, AccessTokenDto providerAccessTokenDto) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("A_auth_provider", providerAlias)
                .addValue("A_app_logical_name", clientAlias)
                .addValue("A_provider_user_id", oid)
                .addValue("A_provider_access_token", providerAccessTokenDto.getAccessToken());

        try {
            return jdbcTemplate.queryForObject(CABINET_AUTH_CODE_SQL, params, String.class);
        } catch (DataAccessException e) {
            throw new CabinetProfileServiceException("Could not getCabinetAuthorizationCode {oid=" + oid + '}', e);
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



    private boolean checkProfileRegistrationRequired(String providerAlias, long oid) {
        String sql = "select wpms2_auth_wp.need_request_profile(A_provider_user_id => :oid, A_auth_provider => :auth_provider) from dual";

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("oid", oid)
                .addValue("auth_provider", providerAlias);

        try {
            return Integer.valueOf(1).equals(jdbcTemplate.queryForObject(sql, params, Integer.class));
        } catch (Exception e) {
            throw new CabinetProfileServiceException("Could not checkProfileRegistrationRequired {oid=" + oid + '}', e);
        }
    }
}
