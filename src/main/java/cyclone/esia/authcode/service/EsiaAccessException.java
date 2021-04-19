package cyclone.esia.authcode.service;

public class EsiaAccessException extends RuntimeException {
    public EsiaAccessException(String message, Throwable cause) {
        super(message, cause);
    }
}
