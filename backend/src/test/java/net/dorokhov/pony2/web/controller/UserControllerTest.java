package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.user.domain.User;
import net.dorokhov.pony2.api.user.service.UserService;
import net.dorokhov.pony2.api.user.service.command.UserCreationCommand;
import net.dorokhov.pony2.api.user.service.exception.DuplicateEmailException;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.CurrentUserUpdateCommandDto;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.ErrorDto.Code;
import net.dorokhov.pony2.web.dto.UserDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

public class UserControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;
    
    @Autowired
    private UserService userService;

    @Test
    public void shouldGetCurrentUser() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), UserDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(user -> 
                assertThat(user.getEmail()).isEqualTo(ADMIN_EMAIL));
    }

    @Test
    public void shouldUpdateCurrentUser() {

        CurrentUserUpdateCommandDto command = new CurrentUserUpdateCommandDto()
                .setName("newName")
                .setEmail("new@email.com")
                .setOldPassword(ADMIN_PASSWORD)
                .setNewPassword("newPassword")
                .setRepeatNewPassword("newPassword")
                ;
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<UserDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), UserDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(user -> {
            assertThat(user.getId()).isEqualTo(authentication.getUser().getId());
            assertThat(user.getCreationDate()).isEqualTo(authentication.getUser().getCreationDate());
            assertThat(user.getUpdateDate()).isNotNull();
            assertThat(user.getName()).isEqualTo("newName");
            assertThat(user.getEmail()).isEqualTo("new@email.com");
            assertThat(user.getRole()).isEqualTo(authentication.getUser().getRole());
        });
        apiTemplate.authenticate("new@email.com", "newPassword");
    }

    @Test
    public void shouldValidateCurrentUserUpdateCommand() {

        CurrentUserUpdateCommandDto command = new CurrentUserUpdateCommandDto()
                .setName(" ")
                .setEmail("invalidEmail")
                .setOldPassword("")
                .setNewPassword("")
                .setRepeatNewPassword("123")
                ;
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(4);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("name", "email", "oldPassword", "newPassword", "repeatNewPassword");
        });
    }

    @Test
    public void shouldFailValidationOnInvalidPassword() {

        CurrentUserUpdateCommandDto command = new CurrentUserUpdateCommandDto()
                .setName("newName")
                .setEmail("new@email.com")
                .setOldPassword("incorrectPassword")
                .setNewPassword("newPassword")
                .setRepeatNewPassword("newPassword")
                ;
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.VALIDATION);
            assertThat(error.getFieldViolations()).hasSize(1);
            assertThat(error.getFieldViolations()).first().satisfies(fieldViolation -> 
                    assertThat(fieldViolation.getField()).isEqualTo("oldPassword"));
        });
    }

    @Test
    public void shouldFailValidationOnDuplicateEmail() throws DuplicateEmailException {

        User user = userService.create(new UserCreationCommand()
                .setName("Plain User")
                .setEmail("new@email.com")
                .setPassword("foobar")
                .setRoles(Set.of(User.Role.USER)));
        CurrentUserUpdateCommandDto command = new CurrentUserUpdateCommandDto()
                .setName("newName")
                .setEmail(user.getEmail())
                .setOldPassword(ADMIN_PASSWORD)
                .setNewPassword("newPassword")
                .setRepeatNewPassword("newPassword")
                ;
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/user", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(command, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.VALIDATION);
            assertThat(error.getFieldViolations()).hasSize(1);
            assertThat(error.getFieldViolations()).first().satisfies(fieldViolation ->
                    assertThat(fieldViolation.getField()).isEqualTo("email"));
        });
    }
}