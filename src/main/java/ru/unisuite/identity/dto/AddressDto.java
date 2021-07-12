package ru.unisuite.identity.dto;

import lombok.Data;

/**
 * Сведения об отдельной записи в перечне адресов физического лица
 * Адреса:
 * <type> – тип записи, может иметь значения:
 * - "PLV" – адрес места проживания;
 * - "PRG" – адрес места регистрации.
 * <zipCode> – индекс;
 * <countryId> – идентификатор страны (3-х символьный код страны по справочнику ОКСМ, например "RUS");
 * <addressStr> – адрес в виде строки (не включая дом, строение, корпус, номер квартиры);
 * <building> – строение;
 * <frame> – корпус;
 * <house> – дом;
 * <flat> – квартира;
 * <fiasCode> – код КЛАДР;
 * <region> – регион;
 * <city> – город;
 * <district> – внутригородской район;
 * <area> – район;
 * <settlement> – поселение;
 * <additionArea> – доп. территория;
 * <additionAreaStreet> – улица на доп. территории;
 * <street> – улица
 */

@Data
public class AddressDto {

    private Type type;
    private String zipCode;
    private String countryId;
    private String addressStr;
    private String building;
    private String frame;
    private String house;
    private String flat;
    private String fiasCode;
    private String region;
    private String city;
    private String district;
    private String area;
    private String settlement;
    private String additionArea;
    private String additionAreaStreet;
    private String street;

    enum Type {
        PLV, PRG
    }

}
