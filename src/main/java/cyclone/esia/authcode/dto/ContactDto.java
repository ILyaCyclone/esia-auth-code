package cyclone.esia.authcode.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

/**
 * Сведения об отдельной записи в перечне контактов физического лица Контактные данные:
 * <type> – тип записи, может иметь значения:
 * - "MBT" – мобильный телефон;
 * - "PHN" – домашний телефон;
 * - "EML" – электронная почта.
 * <vrfStu> – сведения о «подтвержденности» контактов, может иметь значения:
 * - "NOT_VERIFIED" – не подтвержден;
 * - "VERIFIED" – подтвержден.
 *      В настоящее время статус "VERIFIED" может быть только у мобильного телефона ("MBT") и адреса электронной почты
 *      ("EML").
 * <value> – значение контакта;
 * <vrfValStu> – необязательный параметр, указывается в случае, если контакт находится в процессе подтверждения. Может
 *      принимать следующее значение:
 * - "VERIFYING" – в процессе подтверждения. В настоящее время статус "VERIFYING" может быть только у мобильного
 *      телефона ("MBT") и адреса электронной почты ("EML").
 * <verifyingValue> – значение контакта, находящегося в процессе подтверждения.
 */

@Data
public class ContactDto {

    private String value;
    private Type type;
    @JsonProperty("vrfStu")
    private VerifiedStatus verifiedStatus;

    public enum Type {
        MBT, PHN, EML
    }

    public enum VerifiedStatus {
        NOT_VERIFIED, VERIFIED
    }
}
