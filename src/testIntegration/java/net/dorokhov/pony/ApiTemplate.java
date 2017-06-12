package net.dorokhov.pony;

import net.dorokhov.pony.web.domain.CredentialsDto;
import net.dorokhov.pony.web.domain.UserTokenDto;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;

import static net.dorokhov.pony.InstallingIntegrationTest.ADMIN_EMAIL;
import static net.dorokhov.pony.InstallingIntegrationTest.ADMIN_PASSWORD;

@Component
public class ApiTemplate {
    
    private final TestRestTemplate restTemplate;

    public ApiTemplate(TestRestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public UserTokenDto authenticateAdmin() {
        return authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public UserTokenDto authenticate(String email, String password) {
        return restTemplate.postForEntity("/api/authentication", new CredentialsDto(email, password), UserTokenDto.class).getBody();
    }
    
    public HttpEntity<Void> createHeaderRequest(String token) {
        return createHeaderRequest(null, token);
    }

    public <T> HttpEntity<T> createHeaderRequest(T request, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(request, headers);
    }
    
    public HttpEntity<Void> createCookieRequest(String token) {
        return createCookieRequest(null, token);
    }

    public <T> HttpEntity<T> createCookieRequest(T request, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.add("Cookie", "PONY_TOKEN=" + token);
        return new HttpEntity<>(request, headers);
    }
}
