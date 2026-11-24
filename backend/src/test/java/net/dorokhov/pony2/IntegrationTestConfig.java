package net.dorokhov.pony2;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.boot.http.converter.autoconfigure.ClientHttpMessageConvertersCustomizer;
import org.springframework.boot.test.http.server.LocalTestWebServer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

@Configuration
@Profile("test")
public class IntegrationTestConfig {

    @Bean
    public RestClient restClient(
            ApplicationContext applicationContext,
            ObjectProvider<ClientHttpMessageConvertersCustomizer> messageConvertersCustomizers
    ) {
        return RestClient.builder()
                .requestFactory(new HttpComponentsClientHttpRequestFactory())
                .uriBuilderFactory(LocalTestWebServer.obtain(applicationContext).uriBuilderFactory())
                .defaultStatusHandler(status -> true, (request, response) -> {})
                .configureMessageConverters(builder -> messageConvertersCustomizers.orderedStream()
                        .forEach(customizer -> customizer.customize(builder)))
                .build();
    }
}
