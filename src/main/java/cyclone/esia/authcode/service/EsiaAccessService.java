package cyclone.esia.authcode.service;

import cyclone.esia.authcode.dto.AccessTokenDto;

public interface EsiaAccessService {
    String generateAuthCodeUrl();

    AccessTokenDto getAccessToken(String authenticationCode);
}
