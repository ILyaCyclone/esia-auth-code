package ru.unisuite.identity.cabinet;

public interface CabinetProfileService {

    CabinetAuthorizationDto getCabinetAuthorizationCode(String esiaAuthorizationCode);

}
