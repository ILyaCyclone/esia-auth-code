package cyclone.esia.authcode.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Данные о физическом лице:
 * <rIdDoc> – идентификатор текущего документа пользователя;
 * <firstName> – имя;
 * <lastName> – фамилия;
 * <middleName> – отчество;
 * <birthDate> – дата рождения (в формате «ДД.ММ.ГГГГ»);
 * <birthPlace> – место рождения пользователя;
 * <gender> - пол;
 * <trusted> – тип учетной записи (подтверждена (“true”) / не подтверждена (“false”));
 * <citizenship> - гражданство (идентификатор страны гражданства);
 * <snils> – СНИЛС;
 * <inn> – ИНН;
 * <updatedOn> – дата последнего изменения учетной записи пользователя (задается как количество секунд, прошедших с
 *      00:00:00 UTC 1 января 1970 года);
 * <verifying> – процесс проверки данных (true/false);
 * <status> – статус УЗ (Registered – зарегистрирована/Deleted – удалена);
 * <selfEmployed> – информация о самозанятом:
 * - <confirmed> - значение “true” – признак самозанятого есть, “false” – признака самозанятого нет;
 * - <confirmDate> - дата обновления статуса самозанятого;
 * <fmsValid> – валидность документа в ФМС (false – документ неактуален, в таком случае, к параметрам добавляется
 * fmsState; true - документ актуален);
 * <fmsState> – статус документа в ФМС, может иметь значения:
 * - “PERSON_NOT_FOUND” – по указанному в запросе СНИЛС пользователь в БД ИС МВД не найден;
 * - “PASSPORT_NOT_FOUND” – в случае, если в полученном досье есть паспорт гражданина РФ и он имеет DOC_STATUS = 302
 *      и в досье нет паспорта гражданина РФ с DOC_STATUS = 300;
 * - “PASSPORT_INVALID” – в случае, если в полученном досье есть паспорт гражданина РФ и он имеет DOC_STATUS = 301
 *      и его данные (серия, номер) совпадают с указанными в УЗ пользователя, при этом в досье нет паспорта
 *      гражданина РФ с DOC_STATUS = 300;
 * - “AUTO_UPDATE_SUCCESS” – в случае успешного завершения автоматического обновления
 */
@Data
public class PersonalDataDto {
    private Long rIdDoc;
    private String firstName;
    private String lastName;
    private String middleName;
    private String birthPlace;
    private boolean trusted;
    private String citizenship;
    private String snils;
    private String inn;
    private String status;

    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate birthDate; // dd.MM.yyyy
    private Gender gender; // M, F

    @JsonFormat(shape = JsonFormat.Shape.NUMBER)
    private Instant updatedOn;

    enum Gender {
        M, F
    }

}
