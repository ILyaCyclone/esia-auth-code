package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.config.ClientProperties;
import ru.unisuite.identity.config.ClientRegistration;
import ru.unisuite.identity.oauth2.Oauth2Flow;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class OAuth2AuthorizationRequestController {
    private static final Logger logger = LoggerFactory.getLogger(OAuth2AuthorizationRequestController.class);

    private final Oauth2Flow oauth2Flow;
    private final ClientProperties clientProperties;
    private final EsiaProperties esiaProperties;

    // default path according to Spring OAuth2AuthorizationRequestRedirectFilter is "/oauth2/authorization/{registrationId}"
    // TODO "/oauth/{provider}" should be removed
    @GetMapping(path = {"/oauth/{provider}", "/oauth2/authorization/{provider}"})
    public RedirectView redirectToProvider(@PathVariable("provider") String identityProviderAliasParam,
                                           @RequestParam("client") String clientApplicationAliasParam,
                                           @RequestParam(value = "client-path", required = false) String clientPath) {

        ClientRegistration clientRegistration = clientProperties.getRegistration().get(clientApplicationAliasParam);
        Objects.requireNonNull(clientRegistration, "Client application '" + clientApplicationAliasParam + "' not found");

        if (!"esia".equals(identityProviderAliasParam)) {
            throw new IllegalArgumentException("Authentication provider '" + identityProviderAliasParam + "' not found");
        }

//        OauthIdentityProvider identityProvider = providers.get(identityProviderAliasParam);
//        OauthClientApplication clientApplication = clients.get(clientApplicationAliasParam);
//
//        Objects.requireNonNull(identityProvider, "Identity provider '" + identityProviderAliasParam + "' not found");
//        Objects.requireNonNull(clientApplication, "Client application '" + clientApplicationAliasParam + "' not found");
//
//        String identityProviderAlias = identityProvider.getAlias();
//        String clientApplicationAlias = clientApplication.getAlias();
//
//        if (!clientApplication.supportsProvider(identityProviderAlias)) {
//            throw new IdentityException("Client application '" + clientApplicationAlias + " doesn't support identity provider '" + identityProviderAlias + '\'');
//        }

        logger.debug("Provider '{}', client '{}',", identityProviderAliasParam, clientApplicationAliasParam);

        UriComponentsBuilder returnUrlBuilder = UriComponentsBuilder.fromHttpUrl(esiaProperties.getBaseUrl() + "/login/oauth2/code/" + identityProviderAliasParam)
                .queryParam("client", clientApplicationAliasParam);

        if (StringUtils.hasText(clientPath)) {
            returnUrlBuilder.queryParam("client-path", urlEncode(clientPath));
        }

        String returnUrl = returnUrlBuilder.toUriString();

        String authorizationUri = oauth2Flow.generateAuthorizationCodeURL(returnUrl);

        logger.debug("authorizationUri: '{}'", authorizationUri);

        return new RedirectView(authorizationUri);
    }

    private String urlEncode(String s) {
        try {
            return URLEncoder.encode(s, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new RuntimeException("Could not encode string '" + s + '\'', e);
        }
    }

}
