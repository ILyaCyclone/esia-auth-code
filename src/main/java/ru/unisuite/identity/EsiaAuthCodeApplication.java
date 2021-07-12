package ru.unisuite.identity;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({EsiaProperties.class})
public class EsiaAuthCodeApplication {

    public static void main(String[] args) {
        SpringApplication.run(EsiaAuthCodeApplication.class, args);
    }

}
