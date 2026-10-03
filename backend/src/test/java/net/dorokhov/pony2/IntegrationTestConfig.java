package net.dorokhov.pony2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.converter.HttpMessageConverters;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.List;

@Configuration
@Profile("test")
public class IntegrationTestConfig {

    @Bean
    public RestTemplate restTemplate(
            ApplicationContext applicationContext,
            ObjectProvider<ClientHttpMessageConvertersCustomizer> messageConvertersCustomizers
    ) {
        RestTemplate restTemplate = new RestTemplate(new HttpComponentsClientHttpRequestFactory());
        restTemplate.setUriTemplateHandler(LocalTestWebServer.obtain(applicationContext).uriBuilderFactory());
        restTemplate.setErrorHandler(response -> false);
        restTemplate.setMessageConverters(buildMessageConverters(messageConvertersCustomizers));
        return restTemplate;
    }

    private List<HttpMessageConverter<?>> buildMessageConverters(
            ObjectProvider<ClientHttpMessageConvertersCustomizer> messageConvertersCustomizers
    ) {
        HttpMessageConverters.ClientBuilder builder = HttpMessageConverters.forClient();
        messageConvertersCustomizers.orderedStream().forEach(customizer -> customizer.customize(builder));
        List<HttpMessageConverter<?>> messageConverters = new ArrayList<>();
        builder.build().forEach(messageConverters::add);
        return messageConverters;
    }
}
