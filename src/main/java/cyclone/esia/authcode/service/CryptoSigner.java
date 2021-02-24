package cyclone.esia.authcode.service;

public interface CryptoSigner {
    byte[] signPkcs7Detached(String textToSign);
}
