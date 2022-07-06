package ru.unisuite.identity.cabinet;

import lombok.Getter;
import ru.unisuite.identity.dto.PersonalDataDto;

@Getter
public class ProfileIsNotTrustedException extends RuntimeException {

    private final long oid;
    private final PersonalDataDto personalData;

    public ProfileIsNotTrustedException(String message, long oid, PersonalDataDto personalData) {
        super(message);

        this.oid = oid;
        this.personalData = personalData;
    }

}
