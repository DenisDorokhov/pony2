package net.dorokhov.pony2.web.security;

import com.google.common.collect.ImmutableMap;
import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.library.domain.ArtworkFiles;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.core.library.service.artwork.ArtworkStorage;
import net.dorokhov.pony2.core.library.service.artwork.command.FileArtworkStorageCommand;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.InstallationDto;
import net.dorokhov.pony2.web.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.IOException;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityTest extends InstallingIntegrationTest {

    private static final Resource IMAGE_RESOURCE = new ClassPathResource("image.png");

    @Autowired
    private ApiTemplate apiTemplate;
    @Autowired
    private UserService userService;
    @Autowired
    private ArtworkStorage artworkStorage;
    
    private ArtworkFiles artworkFiles;

    @Test
    public void shouldAuthenticate() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        assertThat(authentication.getAccessToken()).isNotNull();
        assertThat(authentication.getUser()).satisfies(user -> 
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    @Test
    public void shouldFailAuthenticationWithInvalidCredentials() {

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(ImmutableMap.of(
                "email", "invalidEmail",
                "password", "invalidPassword"
        ));

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().postForEntity("/api/authentication", entity, ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.AUTHENTICATION_FAILED));
    }

    @Test
    public void shouldLogout() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/authentication", HttpMethod.DELETE, 
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), UserDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(user -> 
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    @Test
    public void shouldFailAuthenticationWithInvalidAccessToken() {

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/authentication", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest("invalidToken"), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.AUTHENTICATION_FAILED));
    }

    @Test
    public void shouldFailAuthenticationWithNoToken() {

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().getForEntity("/api/someResource", ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error -> 
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.AUTHENTICATION_FAILED));
    }

    @Test
    public void shouldDenyAccessToAdminAreaIfRoleIsUser() throws DuplicateEmailException {

        User user = createUser();
        AuthenticationDto authentication = apiTemplate.authenticate(user.getEmail(), "foobar");

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/someResource", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.ACCESS_DENIED));
    }

    @Test
    public void shouldAccessUserAreaIfRoleIsUser() throws DuplicateEmailException {

        User user = createUser();
        AuthenticationDto authentication = apiTemplate.authenticate(user.getEmail(), "foobar");

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.GET, 
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), UserDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAccessAdminAreaIfRoleIsUser() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<InstallationDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/installation", HttpMethod.GET, 
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), InstallationDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAccessStaticContentIfRoleIsUser() throws IOException, DuplicateEmailException {

        User user = createUser();
        AuthenticationDto authentication = apiTemplate.authenticate(user.getEmail(), "foobar");
        artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/file/artwork/small/{artworkId}", HttpMethod.GET, 
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), byte[].class, artworkFiles.getArtwork().getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAccessStaticContentIfRoleIsAdmin() throws IOException {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/file/artwork/small/{artworkId}", HttpMethod.GET, 
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), byte[].class, artworkFiles.getArtwork().getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAccessStaticContentIfRoleIsUserAndTokenIsStatic() throws IOException, DuplicateEmailException {

        User user = createUser();
        AuthenticationDto authentication = apiTemplate.authenticate(user.getEmail(), "foobar");
        artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/file/artwork/small/{artworkId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, artworkFiles.getArtwork().getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldAccessStaticContentIfRoleIsAdminAndTokenIsStatic() throws IOException {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        artworkFiles = artworkStorage.getOrSave(new FileArtworkStorageCommand(IMAGE_RESOURCE.getURI(), IMAGE_RESOURCE.getFile()));

        ResponseEntity<byte[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/file/artwork/small/{artworkId}", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), byte[].class, artworkFiles.getArtwork().getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }

    @Test
    public void shouldDenyAccessToAdminAreaIfTokenIsStatic() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/someResource", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.ACCESS_DENIED));
    }

    @Test
    public void shouldDenyAccessToUserAreaIfTokenIsStatic() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getStaticToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.FORBIDDEN);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.ACCESS_DENIED));
    }

    @Test
    public void shouldDenyAccessIfAccessTokenIsPassedInCookie() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.GET,
                apiTemplate.createCookieRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.AUTHENTICATION_FAILED));
    }

    @Test
    void shouldBlockLoginAfter5LoginAttempts() {

        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");

        HttpEntity<Map<String, String>> entity = new HttpEntity<>(ImmutableMap.of(
                "email", ADMIN_EMAIL,
                "password", ADMIN_PASSWORD
        ));

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().postForEntity("/api/authentication", entity, ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.AUTHENTICATION_FAILED));
    }

    @Test
    void shouldNotBlockLoginAfter4LoginAttempts() {

        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        assertThat(authentication.getAccessToken()).isNotNull();
        assertThat(authentication.getUser()).satisfies(user ->
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    @Test
    void shouldClearLoginAttemptsAfterSuccessfulLogin() {

        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");
        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        assertThat(authentication.getAccessToken()).isNotNull();
        assertThat(authentication.getUser()).satisfies(user ->
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));

        apiTemplate.authenticate("incorrectEmail", "incorrectPassword");

        authentication = apiTemplate.authenticateAdmin();

        assertThat(authentication.getAccessToken()).isNotNull();
        assertThat(authentication.getUser()).satisfies(user ->
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    private User createUser() throws DuplicateEmailException {
        return userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("plainUser@foobar.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));
    }
}
