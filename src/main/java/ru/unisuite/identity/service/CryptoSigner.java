package ru.unisuite.identity.service;

public interface CryptoSigner {
    byte[] signPkcs7Detached(String textToSign);
}
