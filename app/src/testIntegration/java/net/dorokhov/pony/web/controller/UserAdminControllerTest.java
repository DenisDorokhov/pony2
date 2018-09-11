package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.api.user.domain.User;
import net.dorokhov.pony.api.user.service.UserService;
import net.dorokhov.pony.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony.web.domain.*;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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
            assertThat(users[0]).satisfies(userDto -> checkUser(userDto, userService.getAll().get(0)));
        });
    }

    @Test
    public void shouldGetUserById() {

        User user = userService.getAll().get(0);
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

        UserCreationCommandDto command = new UserCreationCommandDto("someName", "some@email.com",
                "somePassword", UserDto.Role.USER);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users", HttpMethod.POST,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), UserDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userDto -> {
            User user = userService.getById(userDto.getId());
            assertThat(user).isNotNull();
            checkUser(userDto, user);
        });
        apiTemplate.authenticate("some@email.com", "somePassword");
    }

    @Test
    public void shouldValidateUserCreationCommand() {

        UserCreationCommandDto command = new UserCreationCommandDto(" ", "invalidEmail", 
                "", null);
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

        UserCreationCommandDto command = new UserCreationCommandDto("someName", ADMIN_EMAIL,
                "somePassword", UserDto.Role.ADMIN);
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

        User user = userService.getAll().get(0);
        UserUpdateCommandDto command = new UserUpdateCommandDto(user.getId(), "someName", "some@email.com",
                "newPassword", UserDto.Role.ADMIN);
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/users/{userId}", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), UserDto.class, user.getId());

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(userDto -> {
            User updatedUser = userService.getById(userDto.getId());
            assertThat(updatedUser).isNotNull();
            checkUser(userDto, updatedUser);
        });
        apiTemplate.authenticate("some@email.com", "newPassword");
    }

    @Test
    public void shouldValidateUserUpdateCommand() {

        User user = userService.getAll().get(0);
        UserUpdateCommandDto command = new UserUpdateCommandDto(user.getId(), " ", "invalidEmail",
                "", null);
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

        UserUpdateCommandDto command = new UserUpdateCommandDto("1000", "someName", "some@email.com",
                "somePassword", UserDto.Role.USER);
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

        User user = userService.getAll().get(0);
        userService.create(UserCreationCommand.builder()
                .name("Plain User")
                .email("new@email.com")
                .password("foobar")
                .roles(User.Role.USER)
                .build());
        UserUpdateCommandDto command = new UserUpdateCommandDto(user.getId(), "someName", "new@email.com",
                "somePassword", UserDto.Role.ADMIN);
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

        User user = userService.create(UserCreationCommand.builder()
                .name("Plain User")
                .email("new@email.com")
                .password("foobar")
                .roles(User.Role.USER)
                .build());
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

        User user = userService.getAll().get(0);
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
