package net.dorokhov.pony2;

import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

@Configuration
@Profile("test")
public class IntegrationTestConfig {

    @Bean
    public RestTemplate restTemplate(ApplicationContext applicationContext) {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        restTemplate.setUriTemplateHandler(LocalTestWebServer.obtain(applicationContext).uriBuilderFactory());
        restTemplate.setErrorHandler(response -> false);
        return restTemplate;
    }
}
