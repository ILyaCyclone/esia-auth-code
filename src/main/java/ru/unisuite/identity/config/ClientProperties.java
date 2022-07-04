package ru.unisuite.identity.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashMap;
import java.util.Map;

@ConfigurationProperties("esia.client")
@Data
public class ClientProperties {

    private final Map<String, ClientRegistration> registration = new HashMap<>();

}
