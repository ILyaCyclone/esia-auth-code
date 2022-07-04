package ru.unisuite.identity.cabinet;

import ru.unisuite.identity.dto.AccessTokenDto;

public interface CabinetProfileService {

    CabinetAuthorizationDto getCabinetAuthorizationCode(String esiaAuthorizationCode);

    String getProfileXml(long oid, AccessTokenDto accessTokenDto);

}
