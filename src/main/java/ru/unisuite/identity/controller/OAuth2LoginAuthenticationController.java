package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.view.RedirectView;
import org.springframework.web.util.UriComponentsBuilder;
import ru.unisuite.identity.cabinet.CabinetAuthorizationDto;
import ru.unisuite.identity.cabinet.CabinetProfileService;
import ru.unisuite.identity.config.ClientProperties;
import ru.unisuite.identity.config.ClientRegistration;
import ru.unisuite.identity.service.ConsentDeclinedException;
import ru.unisuite.identity.service.EsiaAccessException;

import java.util.Objects;

@Controller
@RequiredArgsConstructor
public class OAuth2LoginAuthenticationController {

    private final ClientProperties clientProperties;
    private final CabinetProfileService cabinetProfileService;

    // path according to Spring OAuth2LoginAuthenticationFilter
    @GetMapping("/login/oauth2/code/{provider}")
    public RedirectView redirectToClientApplication(@PathVariable("provider") String identityProviderAliasParam
            , @RequestParam("client") String clientApplicationAliasParam
            , @RequestParam(value = "client-path", required = false) String clientApplicationPath

            , @RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription) {

        ClientRegistration clientRegistration = clientProperties.getRegistration().get(clientApplicationAliasParam);
        Objects.requireNonNull(clientRegistration, "Client application '" + clientApplicationAliasParam + "' not found");

        if (!"esia".equals(identityProviderAliasParam)) {
            throw new IllegalArgumentException("Authentication provider '" + identityProviderAliasParam + "' not found");
        }

        if (!StringUtils.hasText(authorizationCode)) {

            if (errorDescription != null && errorDescription.contains("ESIA-007004")) {
                throw new ConsentDeclinedException();
            }

            throw new EsiaAccessException("Authorization code not received. Error: '" + error
                    + "'. Description: '" + errorDescription + "'.");
        }

        CabinetAuthorizationDto cabinetAuthorizationCodeDto = cabinetProfileService
                .getCabinetAuthorizationCode(identityProviderAliasParam, clientApplicationAliasParam, authorizationCode);

        UriComponentsBuilder clientApplicationUrlBuilder = UriComponentsBuilder.fromHttpUrl(clientRegistration.getRedirectUri())
                .queryParam("token", cabinetAuthorizationCodeDto.getAuthorizationCode())
                .queryParam("userId", cabinetAuthorizationCodeDto.getEsiaOid())
                .queryParam("provider", identityProviderAliasParam);

        if (StringUtils.hasText(clientApplicationPath)) {
            clientApplicationUrlBuilder.queryParam("path", clientApplicationPath);
        }

        String clientApplicationUrl = clientApplicationUrlBuilder.toUriString();

        return new RedirectView(clientApplicationUrl);
    }

}
