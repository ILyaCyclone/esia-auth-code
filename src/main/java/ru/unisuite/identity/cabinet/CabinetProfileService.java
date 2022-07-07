package ru.unisuite.identity.cabinet;

import ru.unisuite.identity.dto.AccessTokenDto;

public interface CabinetProfileService {

    CabinetAuthorizationDto getCabinetAuthorizationCode(String esiaAuthorizationCode);

    String fetchProfileXml(long oid, AccessTokenDto accessTokenDto);

}