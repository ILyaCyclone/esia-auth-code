package ru.unisuite.identity.controller;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;
import ru.unisuite.identity.EsiaProperties;
import ru.unisuite.identity.service.CabinetAuthorizationDto;
import ru.unisuite.identity.service.CabinetProfileService;
import ru.unisuite.identity.service.EsiaAccessException;

@Controller
@RequiredArgsConstructor
public class EsiaReturnController {
    private static final Logger logger = LoggerFactory.getLogger(EsiaReturnController.class);

    private final CabinetProfileService cabinetProfileService;
    private final EsiaProperties esiaProperties;

    @GetMapping(path = "/login/oauth2/code/esia", produces = "text/plain")
    public String handleReturn(
            @RequestParam(name = "code", required = false) String authorizationCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        if (!StringUtils.hasText(authorizationCode)) {
            throw new EsiaAccessException("Authorization code not received. Error: '" + error
                    + "'. Description: '" + errorDescription + "'.");
        }

        CabinetAuthorizationDto cabinetAuthorizationCodeDto = cabinetProfileService.getCabinetAuthorizationCode(authorizationCode);

        String cabinetUrl = UriComponentsBuilder.fromHttpUrl(esiaProperties.getCabinetRedirectUrl())
                .queryParam("token", cabinetAuthorizationCodeDto.getAuthorizationCode())
                .queryParam("userId", cabinetAuthorizationCodeDto.getEsiaOid())
                .queryParam("provider", "esia").toUriString();

        return "redirect:" + cabinetUrl;
    }
}
