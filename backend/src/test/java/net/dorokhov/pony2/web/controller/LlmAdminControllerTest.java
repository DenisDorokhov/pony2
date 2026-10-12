package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.llm.service.DiscoveryCacheService;
import net.dorokhov.pony2.core.llm.repository.DiscoveryCacheRepository;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static net.dorokhov.pony2.api.llm.domain.DiscoveryCacheRegion.SPOTIFY;
import static org.assertj.core.api.Assertions.assertThat;

public class LlmAdminControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private DiscoveryCacheService discoveryCacheService;

    @Autowired
    private DiscoveryCacheRepository discoveryCacheRepository;

    @Test
    public void shouldClearDiscoveryCache() {

        discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/discovery/cache", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), Void.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(discoveryCacheRepository.count()).isZero();
    }

    @Test
    public void shouldClearDiscoveryCacheRegion() {

        discoveryCacheService.put(SPOTIFY, "someKey", 1, "someValue");
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/discovery/cache?region={region}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), Void.class, SPOTIFY.name());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(discoveryCacheRepository.count()).isZero();
    }
}
