package ru.unisuite.identity.service;

import org.springframework.stereotype.Service;
import ru.unisuite.identity.EsiaProperties;

import java.io.FileInputStream;
import java.io.IOException;
import java.security.PublicKey;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;

@Service
public class EsiaPublicKeyProvider {
    private final PublicKey publicKey;

    public EsiaPublicKeyProvider(EsiaProperties esiaProperties) throws IOException, CertificateException {
        try (FileInputStream fis = new FileInputStream(esiaProperties.getCertificatePath())) {
            CertificateFactory certificateFactory = CertificateFactory.getInstance("X.509");
            X509Certificate certificate = (X509Certificate) certificateFactory.generateCertificate(fis);
            publicKey = certificate.getPublicKey();
        }
    }

    public PublicKey getPublicKey() {
        return publicKey;
    }
}
