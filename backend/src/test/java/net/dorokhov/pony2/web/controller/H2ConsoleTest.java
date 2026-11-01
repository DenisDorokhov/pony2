package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.IntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.env.Environment;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import static org.assertj.core.api.Assertions.assertThat;

@ActiveProfiles(value = "test", inheritProfiles = false)
@TestPropertySource(properties = {
        "spring.h2.console.enabled=true",
        "spring.jpa.properties.hibernate.search.backend.directory.root=${pony.home}/h2-console-index"
})
class H2ConsoleTest extends IntegrationTest {

    @Autowired private ApiTemplate apiTemplate;
    @Autowired private Environment environment;

    @Test
    void shouldServeH2ConsoleOutsideDevProfile() {
        assertThat(environment.getActiveProfiles()).doesNotContain("dev");

        ResponseEntity<String> response = apiTemplate.getRestTemplate().getForEntity("/h2-console/", String.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).contains("H2 Console");
        assertThat(response.getHeaders().getFirst("X-Frame-Options")).isEqualTo("SAMEORIGIN");
    }
}
