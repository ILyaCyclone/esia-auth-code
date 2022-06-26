package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.unisuite.identity.oauth2.AuthorizationCodeURLProviderImplV1;
import ru.unisuite.identity.oauth2.AuthorizationCodeURLProviderImplV2;

@Controller
@RequiredArgsConstructor
public class RedirectLoginController {

    private final AuthorizationCodeURLProviderImplV1 authorizationCodeURLProviderImplV1;
    private final AuthorizationCodeURLProviderImplV2 authorizationCodeURLProviderImplV2;

    @GetMapping("/oauth/esia/v1-demo")
    public String redirectEsiaAuthV1Demo() {
        return "redirect:" + authorizationCodeURLProviderImplV1.generateAuthorizationCodeURL();
    }

    @GetMapping("/oauth/esia/v2-demo")
    public String redirectEsiaAuthV2Demo() {
        return "redirect:" + authorizationCodeURLProviderImplV2.generateAuthorizationCodeURL();
    }
}
