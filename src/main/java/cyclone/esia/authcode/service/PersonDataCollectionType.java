package cyclone.esia.authcode.service;

public enum  PersonDataCollectionType {
    CONTACTS("ctts"), ADDRESSES("addrs"), DOCUMENTS("docs");

    private final String urlPart;

    PersonDataCollectionType(String urlPart) {
        this.urlPart = urlPart;
    }

    public String urlPart() { return urlPart; }
}
