package ru.unisuite.identity.service;

import io.jsonwebtoken.*;
import lombok.Data;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.SqlParameter;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.jdbc.core.namedparam.SqlParameterSource;
import org.springframework.jdbc.core.simple.SimpleJdbcCall;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.dto.AccessTokenDto;

import javax.annotation.PostConstruct;
import java.sql.Types;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class CabinetProfileServiceImpl implements CabinetProfileService {
    private static final Logger logger = LoggerFactory.getLogger(CabinetProfileServiceImpl.class);

    private static final String SERVICE_NAME = "cabinet";
    private static final String AUTH_PROVIDER = "esia";

    private final EsiaAccessService esiaAccessService;
    private final PersonalDataService personalDataService;
    private final EsiaPublicKeyProvider esiaPublicKeyProvider;
    private final EsiaProperties esiaProperties;

    private JwtParser jwtParser;

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

    private final NamedParameterJdbcTemplate jdbcTemplate;
    //    private final SimpleJdbcCall getCabinetAuthorizationCodeJdbcCall;
    private final String CABINET_AUTH_CODE_SQL = "select wpms2_auth_wp.create_access_token(" +
            "  A_provider_user_id      => :A_provider_user_id " +
            ", A_auth_provider         => :A_auth_provider " +
            ", A_app_logical_name      => :A_app_logical_name " +
            ", A_provider_access_token => :A_provider_access_token " +
            ") from dual";
    private final SimpleJdbcCall registerProfileJdbcCall;


    private final Map<String, Object> createAuthCodeCommonParams;

    public CabinetProfileServiceImpl(EsiaAccessService esiaAccessService, PersonalDataService personalDataService
            , EsiaPublicKeyProvider esiaPublicKeyProvider, EsiaProperties esiaProperties, NamedParameterJdbcTemplate jdbcTemplate) {

        this.esiaAccessService = esiaAccessService;
        this.personalDataService = personalDataService;
        this.esiaPublicKeyProvider = esiaPublicKeyProvider;
        this.esiaProperties = esiaProperties;
        this.jdbcTemplate = jdbcTemplate;

//        this.getCabinetAuthorizationCodeJdbcCall = new SimpleJdbcCall(jdbcTemplate.getJdbcTemplate())
//                .withoutProcedureColumnMetaDataAccess()
////                .withSchemaName("mwpr1")
//                .withCatalogName("wpms2_auth_wp")
//                .withFunctionName("create_access_token")
////                .withReturnValue()
//                .withNamedBinding()
//                .declareParameters(
//                        new SqlParameter("A_provider_user_id", Types.BIGINT)
//                        , new SqlParameter("A_auth_provider", Types.VARCHAR)
//                        , new SqlParameter("A_app_logical_name", Types.VARCHAR)
//                        , new SqlParameter("A_provider_access_token", Types.VARCHAR)
//                );

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

        Map<String, Object> createAuthCodeCommonParamsLocal = new HashMap<>();
        createAuthCodeCommonParamsLocal.put("A_auth_provider", AUTH_PROVIDER);
        createAuthCodeCommonParamsLocal.put("A_app_logical_name", SERVICE_NAME);
        this.createAuthCodeCommonParams = Collections.unmodifiableMap(createAuthCodeCommonParamsLocal);
    }

    @Override
    public CabinetAuthorizationDto getCabinetAuthorizationCode(String esiaAuthorizationCode, String error, String errorDescription) {
        if (!StringUtils.hasText(esiaAuthorizationCode)) {
            logger.warn("Didn't receive authorization code {error: '{}', errorDescription: '{}'}", error, errorDescription);
            throw new RuntimeException("Не удалось получить код авторизации: " + error + " " + errorDescription);
        }

        AccessTokenDto esiaAccessTokenDto = esiaAccessService.getAccessToken(esiaAuthorizationCode);

        Jwt<Header, Claims> accessTokenJwt = parseAndCheckAccessTokenJwt(esiaAccessTokenDto.getAccessToken());

        long oid = accessTokenJwt.getBody().get("urn:esia:sbj_id", Long.class);

        boolean profileFetchRequired = isProfileFetchRequired(oid);

        if (profileFetchRequired) {
            String profileXml = personalDataService.getProfileXml(oid, esiaAccessTokenDto);
            registerProfile(oid, profileXml, "");
        }

        String cabinetAuthorizationCode = getCabinetAuthorizationCode(oid, esiaAccessTokenDto);

        return new CabinetAuthorizationDto(oid, cabinetAuthorizationCode);

//        return "oid: "+oid+"; profileFetchRequired: "+profileFetchRequired;
    }

    private void registerProfile(long oid, String profileXml, String technicalLog) {
        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("A_esia_oid", oid)
                .addValue("A_data", profileXml)
                .addValue("A_log_request", technicalLog);
        registerProfileJdbcCall.execute(params);
    }

    private String getCabinetAuthorizationCode(long oid, AccessTokenDto providerAccessTokenDto) {
        SqlParameterSource params = new MapSqlParameterSource(createAuthCodeCommonParams)
                .addValue("A_provider_user_id", oid)
                .addValue("A_provider_access_token", providerAccessTokenDto.getAccessToken());

//        return getCabinetAuthorizationCodeJdbcCall.executeFunction(String.class, params);
        return jdbcTemplate.queryForObject(CABINET_AUTH_CODE_SQL, params, String.class);
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



    private boolean isProfileFetchRequired(long oid) {
        String sql = "select wpms2_auth_wp.need_request_profile(A_provider_user_id => :oid, A_auth_provider => :auth_provider) from dual";

        SqlParameterSource params = new MapSqlParameterSource()
                .addValue("oid", oid)
                .addValue("auth_provider", AUTH_PROVIDER);

        return jdbcTemplate.queryForObject(sql, params, Integer.class) == 1;
    }
}
