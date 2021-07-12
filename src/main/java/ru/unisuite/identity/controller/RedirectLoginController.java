package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.unisuite.identity.service.EsiaAccessService;

@Controller
@RequiredArgsConstructor
public class RedirectLoginController {

    private final EsiaAccessService esiaAuthUrlService;

    @GetMapping("/oauth/esia")
    public String redirectEsiaAuth() {

        String esiaAuthUrl = esiaAuthUrlService.generateAuthCodeUrl();

        return "redirect:" + esiaAuthUrl;
    }
}
