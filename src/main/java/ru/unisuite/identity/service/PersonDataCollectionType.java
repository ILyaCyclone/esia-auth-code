package ru.unisuite.identity.service;

/**
 * ctts – контактные данные;
 * addrs – адреса, получение адреса временной регистрации возможно только с версией API v2;
 * docs – документы пользователя;
 * orgs – организации, сотрудником которых является данный пользователь;
 * kids – дети пользователя;
 * vhls – транспортные средства пользователя.
 */
public enum  PersonDataCollectionType {
    CONTACTS("ctts"), ADDRESSES("addrs"), DOCUMENTS("docs");

    private final String urlPart;

    PersonDataCollectionType(String urlPart) {
        this.urlPart = urlPart;
    }

    public String urlPart() { return urlPart; }
}
