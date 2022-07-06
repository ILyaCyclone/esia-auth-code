package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.service.EsiaAccessException;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Controller
@RequiredArgsConstructor
public class LogoutController {

    private final EsiaProperties esiaProperties;

    @GetMapping("/logout")
    public String logout() {
//        String returnUrl = esiaProperties.getBaseUrl();
//        String returnUrlEncoded = urlEncode(returnUrl);

        return "redirect:" + esiaProperties.getEsiaBaseUrl() + "/idp/ext/Logout?client_id=" + esiaProperties.getClientId();
//                + "&redirect_url=" + returnUrlEncoded;
    }

    private String urlEncode(String string) {
        try {
            return URLEncoder.encode(string, StandardCharsets.UTF_8.name());
        } catch (UnsupportedEncodingException e) {
            throw new EsiaAccessException("Could not encode string '" + string + '\'', e);
        }
    }


}
