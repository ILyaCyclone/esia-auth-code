package ru.unisuite.identity.oauth2;

public interface AuthorizationCodeURLProvider {

    String generateAuthorizationCodeURL(String returnUrl);

}