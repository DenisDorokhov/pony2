package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.web.domain.CredentialsDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import net.dorokhov.pony.web.domain.UserDto;
import net.dorokhov.pony.web.domain.UserTokenDto;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;

import static org.assertj.core.api.Assertions.assertThat;

public class AuthenticationControllerTest extends InstallingIntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Test
    public void shouldAuthenticate() throws Exception {
        ResponseEntity<UserTokenDto> response = authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userToken -> {
            assertThat(userToken.getToken()).isNotNull();
            assertThat(userToken.getUser()).satisfies(this::checkAdminUser);
        });
    }

    @Test
    public void shouldGetCurrentUser() throws Exception {
        ResponseEntity<UserTokenDto> authenticationResponse = authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
        ResponseEntity<UserDto> response = restTemplate.exchange("/api/authentication", HttpMethod.GET,
                createAuthorizedRequest((Void) null, authenticationResponse.getBody().getToken()), UserDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(this::checkAdminUser);
    }

    @Test
    public void shouldLogout() throws Exception {
        ResponseEntity<UserTokenDto> authenticationResponse = authenticate(ADMIN_EMAIL, ADMIN_PASSWORD);
        ResponseEntity<UserDto> response = restTemplate.exchange("/api/authentication", HttpMethod.DELETE,
                createAuthorizedRequest((Void) null, authenticationResponse.getBody().getToken()), UserDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(this::checkAdminUser);
    }

    @Test
    public void shouldValidateCredentials() throws Exception {
        CredentialsDto credentials = new CredentialsDto(" ", " ");
        ResponseEntity<ErrorDto> response = restTemplate.postForEntity("/api/authentication", credentials, ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(2);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("email", "password");
        });
    }

    @Test
    public void shouldFailAuthenticationOnInvalidCredentials() throws Exception {
        CredentialsDto credentials = new CredentialsDto("invalid@email.com", "invalidPassword");
        ResponseEntity<ErrorDto> response = restTemplate.postForEntity("/api/authentication", credentials, ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(Code.INVALID_CREDENTIALS));
    }

    @Test
    public void shouldFailGettingCurrentUserIfTokenIsInvalid() throws Exception {
        ResponseEntity<ErrorDto> response = restTemplate.exchange("/api/authentication", HttpMethod.DELETE,
                createAuthorizedRequest((Void) null, "invalidToken"), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(Code.ACCESS_DENIED));
    }

    @Test
    public void shouldFailGettingCurrentUserIfNotAuthenticated() throws Exception {
        ResponseEntity<ErrorDto> response = restTemplate.getForEntity("/api/authentication", ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(Code.ACCESS_DENIED));
    }

    @Test
    public void shouldFailLogoutIfNotAuthenticated() throws Exception {
        ResponseEntity<ErrorDto> response = restTemplate.exchange("/api/authentication", HttpMethod.DELETE, new HttpEntity<>((Void) null), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(Code.ACCESS_DENIED));
    }

    private void checkAdminUser(UserDto user) {
        assertThat(user.getCreationDate()).isNotNull();
        assertThat(user.getUpdateDate()).isNull();
        assertThat(user.getName()).isEqualTo(ADMIN_NAME);
        assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL);
        assertThat(user.getRole()).isSameAs(UserDto.Role.ADMIN);
    }

    private ResponseEntity<UserTokenDto> authenticate(String email, String password) {
        return restTemplate.postForEntity("/api/authentication", new CredentialsDto(email, password), UserTokenDto.class);
    }

    private <T> HttpEntity<T> createAuthorizedRequest(T request, String token) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + token);
        return new HttpEntity<>(request, headers);
    }
}