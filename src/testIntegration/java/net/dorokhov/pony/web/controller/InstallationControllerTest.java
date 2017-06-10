package net.dorokhov.pony.web.controller;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony.IntegrationTest;
import net.dorokhov.pony.config.service.ConfigService;
import net.dorokhov.pony.installation.service.InstallationService;
import net.dorokhov.pony.user.domain.User;
import net.dorokhov.pony.user.domain.User.Role;
import net.dorokhov.pony.user.service.UserService;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.ErrorDto.Code;
import net.dorokhov.pony.web.domain.InstallationCommandDto;
import net.dorokhov.pony.web.domain.InstallationCommandDto.LibraryFolder;
import net.dorokhov.pony.web.domain.InstallationDto;
import net.dorokhov.pony.web.service.InstallationSecretManager;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class InstallationControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InstallationSecretManager installationSecretManager;
    @Autowired
    private UserService userService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private InstallationService installationService;

    @Test
    public void shouldValidateInstallationCommand() throws Exception {

        InstallationCommandDto command = new InstallationCommandDto(
                "invalidSecret",
                ImmutableList.of(new LibraryFolder("notExistingFile")),
                " ",
                "invalidEmail",
                " "
        );

        ResponseEntity<ErrorDto> response = restTemplate.postForEntity("/api/installation", command, ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(5);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder("installationSecret", "libraryFolders[0].path", "adminName", "adminEmail", "adminPassword");
        });
    }

    @Test
    public void shouldInstall() throws Exception {

        InstallationCommandDto command = new InstallationCommandDto(
                installationSecretManager.fetchInstallationSecret(),
                emptyList(),
                "Foo Bar",
                "foo@bar.com",
                "somePassword"
        );

        ResponseEntity<InstallationDto> response = restTemplate.postForEntity("/api/installation", command, InstallationDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(installationDto -> 
                assertThat(installationDto.getVersion()).isNotNull());

        assertThat(configService.getAutoScanInterval()).isNull();
        assertThat(configService.getLibraryFolders()).isEmpty();

        Page<User> users = userService.getAll(new PageRequest(0, 100));
        assertThat(users.getTotalElements()).isEqualTo(1);
        assertThat(users.getContent()).first().satisfies(user -> {
            assertThat(user.getName()).isEqualTo("Foo Bar");
            assertThat(user.getEmail()).isEqualTo("foo@bar.com");
            assertThat(user.getPassword()).isNotNull();
            assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
        });

        assertThat(installationService.getInstallation()).satisfies(installation -> 
                assertThat(installation.getVersion()).isNotNull());
    }

    @Test
    public void shouldFailIfAlreadyInstalled() throws Exception {

        InstallationCommandDto command = new InstallationCommandDto(
                installationSecretManager.fetchInstallationSecret(),
                emptyList(),
                "Foo Bar",
                "foo@bar.com",
                "somePassword"
        );

        restTemplate.postForEntity("/api/installation", command, Void.class);
        ResponseEntity<ErrorDto> response = restTemplate.postForEntity("/api/installation", command, ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(errorDto -> 
                assertThat(errorDto.getCode()).isSameAs(Code.BAD_REQUEST));
    }
}
