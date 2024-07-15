package net.dorokhov.pony3;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.web.client.LocalHostUriTemplateHandler;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.http.converter.json.MappingJackson2HttpMessageConverter;

@Configuration
@Profile("test")
public class IntegrationTestConfig {

    private final Environment environment;

    public IntegrationTestConfig(Environment environment) {
        this.environment = environment;
    }

    @Bean
    public TestRestTemplate restTemplate(ObjectMapper objectMapper) {
        
        TestRestTemplate restTemplate = new TestRestTemplate();
        restTemplate.setUriTemplateHandler(new LocalHostUriTemplateHandler(environment));

        MappingJackson2HttpMessageConverter messageConverter = new MappingJackson2HttpMessageConverter();
        messageConverter.setObjectMapper(objectMapper);
        restTemplate.getRestTemplate().getMessageConverters().removeIf(httpMessageConverter ->
                httpMessageConverter instanceof MappingJackson2HttpMessageConverter);
        restTemplate.getRestTemplate().getMessageConverters().add(messageConverter);

        return restTemplate;
    }
}
