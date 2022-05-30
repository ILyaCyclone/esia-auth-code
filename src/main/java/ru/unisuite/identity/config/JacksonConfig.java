package ru.unisuite.identity.config;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.xml.XmlMapper;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;

@Configuration
public class JacksonConfig {

    @Bean
    public XmlMapper xmlMapper() {
//        XmlMapper xmlMapper = new XmlMapper();
//        xmlMapper.registerModule(new JavaTimeModule());
//        xmlMapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
//        return xmlMapper;

        return Jackson2ObjectMapperBuilder.xml().build();

    }

    /**
     * Example of how to create second Jackson object mapper with different configs without overriding Spring Boot defaults
     */
    @Bean
    @Primary
    public ObjectMapper objectMapper() {
        return Jackson2ObjectMapperBuilder.json()
                .failOnUnknownProperties(false)
                .build();
    }
//
//    @Bean
//    public ObjectMapper snakeCaseObjectMapper() {
//        ObjectMapper objectMapper = new ObjectMapper()
//                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
//                .setPropertyNamingStrategy(PropertyNamingStrategy.SNAKE_CASE);
//        return objectMapper;
//    }
//
}
