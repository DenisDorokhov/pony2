package net.dorokhov.pony.web.security;

import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class UnauthorizedEntryPointTest extends IntegrationTest {
    
    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldReturnAccessDeniedApiResponse() throws Exception {
        ResponseEntity<ErrorDto> response = restTemplate.getForEntity("/api/someResource", ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(errorDto -> {
            assertThat(errorDto.getCode()).isSameAs(Code.ACCESS_DENIED);
            assertThat(errorDto.getMessage()).isNotNull();
            assertThat(errorDto.getArguments()).hasSize(0);
            assertThat(errorDto.getFieldViolations()).hasSize(0);
        });
    }

    @Test
    public void shouldReturnAccessDeniedPageResponse() throws Exception {
        ResponseEntity<String> response = restTemplate.getForEntity("/somePage", String.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
    }
}