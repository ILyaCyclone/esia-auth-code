package ru.unisuite.identity.service;

public interface CryptoSigner {
    byte[] signGost2012(String textToSign);

    byte[] signGost2012Pkcs7Detached(String textToSign);
}
