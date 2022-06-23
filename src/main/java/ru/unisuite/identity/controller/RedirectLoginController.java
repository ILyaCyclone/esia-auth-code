package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.unisuite.identity.service.AuthorizationCodeURLProvider;
import ru.unisuite.identity.service.EsiaAccessServiceV1Impl;
import ru.unisuite.identity.service.EsiaAccessServiceV2Impl;

@Controller
@RequiredArgsConstructor
public class RedirectLoginController {

    private final EsiaAccessServiceV1Impl esiaAuthUrlServiceV1;
    private final EsiaAccessServiceV2Impl esiaAuthUrlServiceV2;

    private final AuthorizationCodeURLProvider authorizationCodeURLProvider;

    @GetMapping("/oauth/esia/v1")
    public String redirectEsiaAuthV1() {
        String esiaAuthUrl = esiaAuthUrlServiceV1.generateAuthCodeUrl();
        return "redirect:" + esiaAuthUrl;
    }

    @GetMapping("/oauth/esia/v2")
    public String redirectEsiaAuthV2() {
        String esiaAuthUrl = esiaAuthUrlServiceV2.generateAuthCodeUrl();
        return "redirect:" + esiaAuthUrl;
    }


    @GetMapping("/oauth/esia/v1-demo")
    public String redirectEsiaAuthV1Demo() {
        return "redirect:" + esiaAuthUrlServiceV1.generateAuthCodeUrl();
    }

    @GetMapping("/oauth/esia/v2-demo")
    public String redirectEsiaAuthV2Demo() {
        return "redirect:" + esiaAuthUrlServiceV2.generateAuthCodeUrl();
    }
}
