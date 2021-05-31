package cyclone.esia.authcode.service;

public interface CabinetProfileService {

    CabinetAuthorizationDto getCabinetAuthorizationCode(String esiaAuthorizationCode, String error, String errorDescription);

}
