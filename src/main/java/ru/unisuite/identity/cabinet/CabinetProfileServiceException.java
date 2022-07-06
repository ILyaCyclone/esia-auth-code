package ru.unisuite.identity.cabinet;

public class CabinetProfileServiceException extends RuntimeException {

    public CabinetProfileServiceException(String message) {
        super(message);
    }

    public CabinetProfileServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
