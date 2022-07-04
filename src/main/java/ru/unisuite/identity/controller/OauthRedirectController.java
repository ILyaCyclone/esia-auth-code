package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.config.ClientProperties;
import ru.unisuite.identity.config.ClientRegistration;
import ru.unisuite.identity.oauth2.Oauth2Flow;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class OauthRedirectController {
    private static final Logger logger = LoggerFactory.getLogger(OauthRedirectController.class);

    private final Oauth2Flow oauth2Flow;
    private final ClientProperties clientProperties;
    private final EsiaProperties esiaProperties;

    @GetMapping("/oauth/{provider}")
    public RedirectView redirectToProvider(@PathVariable("provider") String identityProviderAliasParam
            , @RequestParam("client") String clientApplicationAliasParam) {

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

//        String returnUrl = identityProperties.getBaseUrl() + "/login/oauth2/code/" + identityProviderAlias + "?client=" + clientApplicationAlias;
        String returnUrl = esiaProperties.getBaseUrl() + "/login/oauth2/code/" + identityProviderAliasParam + "?client=" + clientApplicationAliasParam;
        String authorizationUri = oauth2Flow.generateAuthorizationCodeURL(returnUrl);

        logger.debug("authorizationUri: '{}'", authorizationUri);

        return new RedirectView(authorizationUri);
    }

}
