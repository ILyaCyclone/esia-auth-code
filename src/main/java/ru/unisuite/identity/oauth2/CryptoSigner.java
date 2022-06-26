package ru.unisuite.identity.oauth2;

public interface CryptoSigner {
    byte[] signGost2012(String textToSign);

    byte[] signGost2012Pkcs7Detached(String textToSign);
}
