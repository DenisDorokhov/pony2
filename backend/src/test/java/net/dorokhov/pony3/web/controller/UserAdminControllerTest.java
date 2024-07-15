package net.dorokhov.pony3.web.controller;

import net.dorokhov.pony3.ApiTemplate;
import net.dorokhov.pony3.InstallingIntegrationTest;
import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.service.UserService;
import net.dorokhov.pony3.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony3.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony3.web.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserAdminControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Autowired
    private UserService userService;

    @Test
    public void shouldGetAllUsers() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto[]> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), UserDto[].class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(users -> {
            assertThat(users).hasSize(1);
            assertThat(users[0]).satisfies(userDto -> checkUser(userDto, userService.getAll().getFirst()));
        });
    }

    @Test
    public void shouldGetUserById() {

        User user = userService.getAll().getFirst();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), UserDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userDto -> checkUser(userDto, user));
    }

    @Test
    public void shouldFailGettingUserByIdOnNotExistingUser() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/1000", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("User");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldCreateUser() {

        UserCreationCommandDto command = new UserCreationCommandDto()
                .setName("someName")
                .setEmail("some@email.com")
                .setPassword("somePassword")
                .setRole(UserDto.Role.USER);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users", HttpMethod.POST,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), UserDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userDto -> {
            User user = userService.getById(userDto.getId()).orElse(null);
            assertThat(user).isNotNull();
            checkUser(userDto, user);
        });
        apiTemplate.authenticate("some@email.com", "somePassword");
    }

    @Test
    public void shouldValidateUserCreationCommand() {

        UserCreationCommandDto command = new UserCreationCommandDto()
                .setName(" ")
                .setEmail("invalidEmail")
                .setPassword("")
                .setRole(null);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users", HttpMethod.POST,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(4);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("name", "email", "password", "role");
        });
    }

    @Test
    public void shouldFailUserCreationValidationOnDuplicateEmail() {

        UserCreationCommandDto command = new UserCreationCommandDto()
                .setName("someName")
                .setEmail(ADMIN_EMAIL)
                .setPassword("somePassword")
                .setRole(UserDto.Role.ADMIN);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users", HttpMethod.POST,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations()).hasSize(1);
            assertThat(error.getFieldViolations()).first().satisfies(fieldViolation ->
                    assertThat(fieldViolation.getField()).isEqualTo("email"));
        });
    }

    @Test
    public void shouldUpdateUser() {

        User user = userService.getAll().getFirst();
        UserUpdateCommandDto command = new UserUpdateCommandDto()
                .setId(user.getId())
                .setName("someName")
                .setEmail("some@email.com")
                .setNewPassword("newPassword")
                .setRole(UserDto.Role.ADMIN);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), UserDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userDto -> {
            User updatedUser = userService.getById(userDto.getId()).orElse(null);
            assertThat(updatedUser).isNotNull();
            checkUser(userDto, updatedUser);
        });
        apiTemplate.authenticate("some@email.com", "newPassword");
    }

    @Test
    public void shouldValidateUserUpdateCommand() {

        User user = userService.getAll().getFirst();
        UserUpdateCommandDto command = new UserUpdateCommandDto()
                .setId(user.getId())
                .setName(" ")
                .setEmail("invalidEmail")
                .setNewPassword("")
                .setRole(null);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(4);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("name", "email", "newPassword", "role");
        });
    }

    @Test
    public void shouldFailUserUpdateOnNotExistingUser() {

        UserUpdateCommandDto command = new UserUpdateCommandDto()
                .setId("1000")
                .setName("someName")
                .setEmail("some@email.com")
                .setNewPassword("somePassword")
                .setRole(UserDto.Role.USER);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/1000", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("User");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldFailUserUpdateValidationOnDuplicateEmail() throws DuplicateEmailException {

        User user = userService.getAll().getFirst();
        userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));
        UserUpdateCommandDto command = new UserUpdateCommandDto()
                .setId(user.getId())
                .setName("someName")
                .setEmail("new@email.com")
                .setNewPassword("somePassword")
                .setRole(UserDto.Role.ADMIN);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations()).hasSize(1);
            assertThat(error.getFieldViolations()).first().satisfies(fieldViolation ->
                    assertThat(fieldViolation.getField()).isEqualTo("email"));
        });
    }

    @Test
    public void shouldDeleteUser() throws DuplicateEmailException {

        User user = userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), UserDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userDto -> checkUser(userDto, user));
    }

    @Test
    public void shouldFailWhenDeletingNotExistingUser() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/1000", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments().get(0)).isEqualTo("User");
            assertThat(error.getArguments().get(1)).isEqualTo("1000");
        });
    }

    @Test
    public void shouldFailWhenDeletingCurrentUser() {

        User user = userService.getAll().getFirst();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.DELETE,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error ->
                assertThat(error.getCode()).isSameAs(ErrorDto.Code.BAD_REQUEST));
    }

    private void checkUser(UserDto dto, User user) {
        assertThat(dto.getId()).isEqualTo(user.getId());
        assertThat(dto.getCreationDate()).isEqualTo(user.getCreationDate());
        assertThat(dto.getUpdateDate()).isEqualTo(user.getUpdateDate());
        assertThat(dto.getName()).isEqualTo(user.getName());
        assertThat(dto.getEmail()).isEqualTo(user.getEmail());
        if (user.getRoles().contains(User.Role.ADMIN)) {
            assertThat(dto.getRole()).isSameAs(UserDto.Role.ADMIN);
        } else {
            assertThat(dto.getRole()).isSameAs(UserDto.Role.USER);
        }
    }
}
