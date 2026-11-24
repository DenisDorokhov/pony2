package net.dorokhov.pony2;

import net.dorokhov.pony2.web.dto.AuthenticationDto;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;

import static net.dorokhov.pony2.InstallingIntegrationTest.ADMIN_EMAIL;
import static net.dorokhov.pony2.InstallingIntegrationTest.ADMIN_PASSWORD;

@Component
public class ApiTemplate {

    private final RestClient restClient;

    public ApiTemplate(RestClient restClient) {
        this.restClient = restClient;
    }

    public RestClient getRestClient() {
        return restClient;
    }

    public AuthenticationDto authenticateAdmin() {
        return authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
    }

    public AuthenticationDto authenticate(String email, String password) {
        MultiValueMap<String, String> params = new LinkedMultiValueMap<>();
        params.add("email", email);
        params.add("password", password);
        return restClient.post()
                .uri("/api/authentication")
                .body(params)
                .retrieve()
                .toEntity(AuthenticationDto.class)
                .getBody();
    }

    public HttpEntity<Void> createHeaderRequest(String token) {
        return createHeaderRequest(null, token);
    }

    public <T> HttpEntity<T> createHeaderRequest(T request, String token) {
        return createHeaderRequest(request, token, new HttpHeaders());
    }

    public <T> HttpEntity<T> createHeaderRequest(T request, String token, HttpHeaders headers) {
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(request, headers);
    }

    public HttpEntity<Void> createCookieRequest(String token) {
        return createCookieRequest(null, token);
    }

    public <T> HttpEntity<T> createCookieRequest(T request, String token) {
        return createCookieRequest(request, token, new HttpHeaders());
    }

    public <T> HttpEntity<T> createCookieRequest(T request, String token, HttpHeaders headers) {
        headers.add("Cookie", "pony2.staticToken=" + token);
        return new HttpEntity<>(request, headers);
    }
}
