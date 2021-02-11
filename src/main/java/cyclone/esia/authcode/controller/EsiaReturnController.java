package cyclone.esia.authcode.controller;

import org.springframework.util.Base64Utils;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.nio.charset.StandardCharsets;
import java.util.StringJoiner;

@RestController
public class EsiaReturnController {

    @GetMapping(path = "/esia_return", produces = "text/plain")
    public String handleReturn(
            @RequestParam(name = "code", required = false) String authCode
            , @RequestParam(name = "error", required = false) String error
            , @RequestParam(name = "error_description", required = false) String errorDescription
    ) {
        StringJoiner joiner = new StringJoiner("\n\n");
        if (StringUtils.hasText(authCode)) {
            joiner.add("сode=" + authCode);

            String[] jwtParts = authCode.split("\\.");
            if (jwtParts.length == 3) {
                joiner.add("try base64decode jwt");
                joiner.add(new String(Base64Utils.decodeFromUrlSafeString(jwtParts[0]), StandardCharsets.UTF_8));
                joiner.add(new String(Base64Utils.decodeFromUrlSafeString(jwtParts[1]), StandardCharsets.UTF_8));
            }
        }
        if (StringUtils.hasText(error)) {
            joiner.add("error=" + error);
        }
        if (StringUtils.hasText(errorDescription)) {
            joiner.add("error_description=" + errorDescription);
        }

        if (joiner.length() == 0) {
            joiner.add("No expected parameters received.");
        }

        return joiner.toString();
    }

}
