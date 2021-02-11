package cyclone.esia.authcode.service;

public interface CryptoSigner {
    byte[] sign(String textToSign);
}
