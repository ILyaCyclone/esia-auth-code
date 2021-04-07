package cyclone.esia.authcode.service;

public class CryptoSignerException extends RuntimeException {
    public CryptoSignerException(String message) {
        super(message);
    }

    public CryptoSignerException(String message, Throwable cause) {
        super(message, cause);
    }
}
