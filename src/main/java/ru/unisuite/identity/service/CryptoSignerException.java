package ru.unisuite.identity.service;

public class CryptoSignerException extends RuntimeException {
    public CryptoSignerException(String message) {
        super(message);
    }

    public CryptoSignerException(String message, Throwable cause) {
        super(message, cause);
    }
}
