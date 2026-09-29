package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.llm.service.LlmCacheService;
import net.dorokhov.pony2.core.llm.repository.LlmCacheRepository;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static net.dorokhov.pony2.api.llm.LlmCacheRegion.SPOTIFY;
import static org.assertj.core.api.Assertions.assertThat;

public class LlmAdminControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private LlmCacheService llmCacheService;

    @Autowired
    private LlmCacheRepository llmCacheRepository;

    @Test
    public void shouldClearLlmCache() {

        llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/llm/cache", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(llmCacheRepository.count()).isZero();
    }

    @Test
    public void shouldClearLlmCacheRegion() {

        llmCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/llm/cache?region={region}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), Void.class, SPOTIFY.name());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(llmCacheRepository.count()).isZero();
    }
}
