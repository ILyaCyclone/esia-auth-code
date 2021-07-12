package ru.unisuite.identity.service;

public interface CabinetProfileService {

    CabinetAuthorizationDto getCabinetAuthorizationCode(String esiaAuthorizationCode, String error, String errorDescription);

}
