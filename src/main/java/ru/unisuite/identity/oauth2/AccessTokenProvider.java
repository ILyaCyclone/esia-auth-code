package ru.unisuite.identity.oauth2;

import ru.unisuite.identity.dto.AccessTokenDto;

public interface AccessTokenProvider {

    AccessTokenDto getAccessToken(String authorizationCode);

}
