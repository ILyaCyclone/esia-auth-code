package ru.unisuite.identity.oauth2;

import ru.unisuite.identity.dto.AccessTokenDto;

public class Oauth2Flow implements AuthorizationCodeURLProvider, AccessTokenProvider {

    private final AuthorizationCodeURLProvider authorizationCodeURLProvider;
    private final AccessTokenProvider accessTokenProvider;

    public Oauth2Flow(AuthorizationCodeURLProvider authorizationCodeURLProvider, AccessTokenProvider accessTokenProvider) {
        this.authorizationCodeURLProvider = authorizationCodeURLProvider;
        this.accessTokenProvider = accessTokenProvider;
    }


    @Override
    public String generateAuthorizationCodeURL() {
        return authorizationCodeURLProvider.generateAuthorizationCodeURL();
    }

    @Override
    public String generateAuthorizationCodeURL(String returnUrl) {
        return authorizationCodeURLProvider.generateAuthorizationCodeURL(returnUrl);
    }

    @Override
    public AccessTokenDto getAccessToken(String authorizationCode) {
        return accessTokenProvider.getAccessToken(authorizationCode);
    }

    @Override
    public AccessTokenDto getAccessToken(String authorizationCode, String returnUrl) {
        return accessTokenProvider.getAccessToken(authorizationCode, returnUrl);
    }

}
