package cyclone.esia.authcode.config;
//
//import com.fasterxml.jackson.databind.DeserializationFeature;
//import com.fasterxml.jackson.databind.ObjectMapper;
//import com.fasterxml.jackson.databind.PropertyNamingStrategy;
//import org.springframework.boot.autoconfigure.jackson.Jackson2ObjectMapperBuilderCustomizer;

import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

//import org.springframework.context.annotation.Primary;
//import org.springframework.http.converter.json.Jackson2ObjectMapperBuilder;
//
@Configuration
public class ApplicationConfig {

    @Bean
    public RestTemplate restTemplate(RestTemplateBuilder restTemplateBuilder) {
        return restTemplateBuilder.build();
    }

    /**
     * Example of how to create second Jackson object mapper with different configs without overriding Spring Boot defaults
     */
//    @Bean
//    @Primary
//    public ObjectMapper objectMapper() {
//        return Jackson2ObjectMapperBuilder.json()
//                .failOnUnknownProperties(false)
//                .build();
//    }
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
