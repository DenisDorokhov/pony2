package net.dorokhov.pony.web.security;

import com.google.common.collect.ImmutableMap;
import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.user.service.command.UserCreationCommand;
import net.dorokhov.pony.user.service.exception.UserExistsException;
import net.dorokhov.pony.web.domain.AuthenticationDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.UserDto;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

public class SecurityTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;
    
    @Autowired
    private UserService userService;

    @Test
    public void shouldAuthenticate() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        assertThat(authentication.getToken()).isNotNull();
        assertThat(authentication.getUser()).satisfies(user -> 
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    @Test
    public void shouldDenyAccessIfAuthenticationFails() throws Exception {
        HttpEntity<Map<String, String>> entity = new HttpEntity<>(ImmutableMap.of(
                "email", "invalidEmail",
                "password", "invalidPassword"
        ));
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().postForEntity("/api/authentication", entity, ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.ACCESS_DENIED));
    }

    @Test
    public void shouldLogout() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/authentication", HttpMethod.DELETE, 
                apiTemplate.createHeaderRequest(authentication.getToken()), UserDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(user -> 
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    @Test
    public void shouldDenyAccessIfLogoutFails() throws Exception {
        ResponseEntity<Void> response = apiTemplate.getRestTemplate().exchange(
                "/api/authentication", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest("invalidToken"), Void.class);
    }

    @Test
    public void shouldDenyAccessToApiResponse() throws Exception {
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().getForEntity("/api/someResource", ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(errorDto -> 
                assertThat(errorDto.getCode()).isSameAs(ErrorDto.Code.ACCESS_DENIED));
    }

    @Test
    public void shouldDenyAdminAreaIfRoleIsUser() throws Exception {
        User user = createUser();
        AuthenticationDto authentication = apiTemplate.authenticate(user.getEmail(), "foobar");
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().getForEntity("/api/admin/someResource", ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).satisfies(errorDto ->
                assertThat(errorDto.getCode()).isSameAs(ErrorDto.Code.ACCESS_DENIED));
    }

    @Test
    public void shouldNotDenyUserAreaIfRoleIsUser() throws Exception {
        User user = createUser();
        AuthenticationDto authentication = apiTemplate.authenticate(user.getEmail(), "foobar");
        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.GET, 
                apiTemplate.createHeaderRequest(authentication.getToken()), UserDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
    }
    
    private User createUser() throws UserExistsException {
        return userService.create(UserCreationCommand.builder()
                .name("Plain User")
                .email("plainUser@foobar.com")
                .password("foobar")
                .roles(User.Role.USER)
                .build());
    }
}
