package ru.unisuite.identity.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import lombok.Data;

@Data
@JsonNaming(PropertyNamingStrategy.SnakeCaseStrategy.class)
public class AccessTokenDto {

    private String accessToken;
    private String refreshToken;

    private Integer expiresIn;
    private String state;
    private String tokenType;

}
