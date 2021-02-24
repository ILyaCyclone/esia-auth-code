package cyclone.esia.authcode.service;

/**
 * 1. fullname Просмотр фамилии, имени и отчества
 * − фамилия;
 * − имя;
 * − отчество.
 * 2. birthdate Просмотр даты рождения − дата рождения, указанная в учетной записи
 * 3. gender Просмотр пола − пол, указанный в учетной записи
 * 4. snils Просмотр СНИЛС − СНИЛС, указанный в учетной записи
 * 5. inn Просмотр ИНН − ИНН, указанный в учетной записи
 * 6. id_doc Просмотр данных о документе, удостоверяющем личность
 * − серия и номер документа, удостоверяющего личность;
 * − дата выдачи;
 * − кем выдан;
 * − код подразделения;
 * − гражданство.
 * 7. birthplace Просмотр места рождения − место рождения.
 * 8. medical_doc Просмотр данных полиса обязательного медицинского страхования (ОМС)
 * − номер полиса ОМС;
 * − срок действия.
 * 9. military_doc Просмотр данных военного билета
 * − серия и номер военного билета;
 * − дата выдачи;
 * − орган, выдавший документ.
 * 10. foreign_passport_doc Просмотр данных заграничного паспорта
 * − фамилия, имя, отчество буквами латинского алфавита;
 * − серия и номер заграничного паспорта;
 * − дата выдачи;
 * − срок действия;
 * − орган, выдавший документ;
 * − гражданство.
 * 11. drivers_licence_doc Просмотр данных водительского удостоверения
 * − серия и номер водительского удостоверения;
 * − дата выдачи;
 * − срок действия.
 * 12. birth_cert_doc Просмотр данных свидетельства о рождении
 * − серия и номер свидетельства;
 * − дата выдачи;
 * − место государственной регистрации.
 * 13. residence_doc Просмотр данных вида на жительство
 * − серия и номер вида на жительство;
 * − дата выдачи.
 * 14. temporary_residence_doc Просмотр данных разрешения на временное проживание
 * − серия и номер разрешения на временное проживание;
 * − дата выдачи.
 * 15. vehicles Просмотр данных транспортных средств
 * − государственный регистрационный знак;
 * − серия и номер свидетельства о регистрации.
 * 16. email Просмотр адреса электронной почты
 * − адрес электронной почты, указанный в учетной записи
 * 17. mobile Просмотр номера мобильного телефона
 * − номер мобильного телефона
 * 18. contacts Просмотр данных о контактах и адресах
 * − номер домашнего телефона;
 * − номер мобильного телефона;
 * − адрес электронной почты;
 * − адрес регистрации;
 * − адрес места
 * проживания.
 * 19. usr_org Просмотр списка организаций пользователя
 * − список организаций пользователя.
 * 20. usr_avt Просмотр изображения (аватара) пользователя
 * − Получения изображения (аватара);
 * − Создание и обновление изображения (аватара);
 * − Получение исходного изображения (аватара)
 * 21. self_employed Просмотр данных о самозанятых
 * − Признак самозанятого
 * − Категория (вид деятельности)
 */
public enum Scope {
    FULLNAME, BIRTHDATE, GENDER
    , SNILS, INN, ID_DOC, BIRTHPLACE
    , MEDICAL_DOC, MILITARY_DOC, FOREIGN_PASSPORT_DOC, DRIVERS_LICENCE_DOC, BIRTH_CERT_DOC, RESIDENCE_DOC, TEMPORARY_RESIDENCE_DOC
    , VEHICLES
    , EMAIL, MOBILE, CONTACTS
    , USR_ORG, USR_AVT
    , SELF_EMPLOYED
}
