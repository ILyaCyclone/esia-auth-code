package ru.unisuite.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.unisuite.identity.config.ClientProperties;

@SpringBootApplication
@EnableConfigurationProperties({EsiaProperties.class, ClientProperties.class})
public class EsiaAuthCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsiaAuthCodeApplication.class, args);
    }

}
