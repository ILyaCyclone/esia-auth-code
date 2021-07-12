package cyclone.esia.authcode.controller;

import cyclone.esia.authcode.EsiaProperties;
import cyclone.esia.authcode.service.CabinetAuthorizationDto;
import cyclone.esia.authcode.service.CabinetProfileService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.util.UriComponentsBuilder;

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
        CabinetAuthorizationDto cabinetAuthorizationCodeDto = cabinetProfileService.getCabinetAuthorizationCode(authorizationCode, error, errorDescription);

        String cabinetUrl = UriComponentsBuilder.fromHttpUrl(esiaProperties.getCabinetRedirectUrl())
                .queryParam("token", cabinetAuthorizationCodeDto.getAuthorizationCode())
                .queryParam("userId", cabinetAuthorizationCodeDto.getEsiaOid())
                .queryParam("provider", "esia").toUriString();

        return "redirect:" + cabinetUrl;
    }
}
