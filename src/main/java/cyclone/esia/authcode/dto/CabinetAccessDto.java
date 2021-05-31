package cyclone.esia.authcode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

@Data
public class CabinetAccessDto {
    @JsonProperty("userId")
    private final String providerUserId;
    @JsonProperty("provider")
    private final String authProvider;
    @JsonProperty("token")
    private final String authorizationCode;
}
