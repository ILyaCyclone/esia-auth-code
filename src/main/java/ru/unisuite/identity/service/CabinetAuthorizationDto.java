package ru.unisuite.identity.service;

import lombok.Data;

@Data
public class CabinetAuthorizationDto {
    private final long esiaOid;
    private final String authorizationCode;
}
