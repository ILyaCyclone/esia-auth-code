package ru.unisuite.identity;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import ru.unisuite.identity.config.ClientProperties;

import javax.annotation.PostConstruct;

@SpringBootApplication
@EnableConfigurationProperties({EsiaProperties.class, ClientProperties.class})
public class EsiaAuthCodeApplication {
    private static final Logger logger = LoggerFactory.getLogger(EsiaAuthCodeApplication.class);

    @Autowired
    private EsiaProperties esiaProperties;

    @PostConstruct
    public void printLogInfo() {
        logger.info("Authentication provider service started with ESIA environment {}", esiaProperties.getEnvironment());
    }


    public static void main(String[] args) {
        SpringApplication.run(EsiaAuthCodeApplication.class, args);
    }

}
