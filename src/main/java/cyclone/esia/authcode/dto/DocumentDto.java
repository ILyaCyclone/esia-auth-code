package cyclone.esia.authcode.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;

import java.time.LocalDate;

/**
 * Сведения об отдельной записи в перечне документов физического лица Документы:
 * <type> – тип записи, может иметь значения:
 * - "RF_PASSPORT" – паспорт гражданина РФ;
 * - "FID_DOC" – документ иностранного гражданина;
 * - "RF_DRIVING_LICENSE" – водительское удостоверение.
 * - "MLTR_ID" – военный билет;
 * - "FRGN_PASS" – заграничный паспорт;
 * - "MDCL_PLCY" – полис ОМС;
 * - "RF_BRTH_CERT" – свидетельство о рождении – Россия;
 * - "FID_BRTH_CERT" – свидетельство о рождении – другая страна;
 * - "OLD_BRTH_CERT" – свидетельство о рождении – СССР.
 * <vrfStu> – сведения о «подтвержденности» документов, может иметь значения:
 * - "NOT_VERIFIED" – не подтвержден;
 * - "VERIFIED" – подтвержден.
 * <actNo> – номер актовой записи (для свидетельства о рождении РФ и СССР);
 * <actDate> - дата актовой записи (только для свидетельства о рождении ребенка РФ);
 * <series> – серия документа;
 * <number> – номер документа;
 * <issueDate> - дата выдачи документа;
 * <issueId> – код подразделения;
 * <issuedBy> – кем выдан;
 * <expiryDate> - срок действия документа;
 * <lastName> – фамилия (для заграничного паспорта);
 * <firstName> – имя (для заграничного паспорта).
 * <vrfValStu> – необязательный параметр, указывается в случае, если документ находится в процессе подтверждения. Может
 * принимать следующее значение:
 * - "VERIFYING" – в процессе подтверждения;
 * - “VERIFICATION_FAILED" – ошибки проверки.
 * <vrfReqId> - идентификатор заявки;
 * <eTag> – тег изменяемого объекта;
 * <fmsValid> – валидность документа в ФМС (false – документ неактуален, в таком случае, к параметрам добавляется
 * fmsState; true - документ актуален);
 * <fmsState> – статус документа в ФМС, может иметь значения:
 * - "PERSON_NOT_FOUND" – по указанному в запросе СНИЛС пользователь в БД ИС МВД не найден;
 * - "PASSPORT_NOT_FOUND" – в случае, если в полученном досье есть паспорт гражданина РФ и он имеет DOC_STATUS = 302 и в
 * досье нет паспорта гражданина РФ с DOC_STATUS = 300;
 * - "PASSPORT_INVALID" – в случае, если в полученном досье есть паспорт гражданина РФ и он имеет DOC_STATUS = 301 и его
 * данные (серия, номер) совпадают с указанными в УЗ пользователя, при этом в досье нет паспорта гражданина РФ с
 * DOC_STATUS = 300;
 * - "AUTO_UPDATE_SUCCESS" – в случае успешного завершения автоматического обновления
 */

@Data
public class DocumentDto {

    private Type type;
    @JsonProperty("vrfStu")
    private VerifiedStatus verifiedStatus;

    private String actNo;
    private String actDate;
    private String series;
    private String number;
    @JsonFormat(pattern = "dd.MM.yyyy")
    private LocalDate issueDate;
    private String issueId;
    private String issuedBy;
    private String expiryDate;
    private String lastName;
    private String firstName;
    private String vrfReqId;
    private String eTag;

    private boolean fmsValid;
    private String fmsState;

    public enum Type {
        RF_PASSPORT, FID_DOC, RF_DRIVING_LICENSE, MLTR_ID, FRGN_PASS, MDCL_PLCY, RF_BRTH_CERT, FID_BRTH_CERT, OLD_BRTH_CERT;
    }

    public enum VerifiedStatus {
        NOT_VERIFIED, VERIFIED
    }

}
