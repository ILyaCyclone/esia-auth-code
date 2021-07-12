package ru.unisuite.identity.service;

import ru.unisuite.identity.dto.AccessTokenDto;

public interface EsiaAccessService {
    String generateAuthCodeUrl();

    AccessTokenDto getAccessToken(String authenticationCode);
}
