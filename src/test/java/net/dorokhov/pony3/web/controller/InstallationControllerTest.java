package net.dorokhov.pony3.web.controller;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony3.IntegrationTest;
import net.dorokhov.pony3.api.config.service.ConfigService;
import net.dorokhov.pony3.api.installation.service.InstallationService;
import net.dorokhov.pony3.api.user.domain.User;
import net.dorokhov.pony3.api.user.domain.User.Role;
import net.dorokhov.pony3.api.user.service.UserService;
import net.dorokhov.pony3.web.dto.*;
import net.dorokhov.pony3.web.dto.ErrorDto.Code;
import net.dorokhov.pony3.web.service.InstallationSecretService;
import net.dorokhov.pony3.web.service.exception.SecretNotFoundException;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.util.List;

import static java.util.Collections.emptyList;
import static org.assertj.core.api.Assertions.assertThat;

public class InstallationControllerTest extends IntegrationTest {

    @Autowired
    private TestRestTemplate restTemplate;

    @Autowired
    private InstallationSecretService installationSecretService;
    @Autowired
    private UserService userService;
    @Autowired
    private ConfigService configService;
    @Autowired
    private InstallationService installationService;

    @Test
    public void shouldReturnNoInstallation() {
        ResponseEntity<InstallationStatusDto> response = restTemplate.getForEntity("/api/installation/status", InstallationStatusDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(installationStatus -> 
                assertThat(installationStatus.isInstalled()).isFalse());
    }

    @Test
    public void shouldInstall() throws SecretNotFoundException, IOException {

        InstallationCommandDto command = new InstallationCommandDto()
                .setInstallationSecret(installationSecretService.fetchInstallationSecret())
                .setLibraryFolders(emptyList())
                .setAdminName("Foo Bar")
                .setAdminEmail("foo@bar.com")
                .setAdminPassword("somePassword")
                .setRepeatAdminPassword("somePassword");

        ResponseEntity<InstallationDto> installationResponse = restTemplate.postForEntity("/api/installation", command, InstallationDto.class);

        assertThat(installationResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(installationResponse.getBody()).satisfies(installationDto -> 
                assertThat(installationDto.getVersion()).isNotNull());

        assertThat(configService.getAutoScanInterval()).isEmpty();
        assertThat(configService.getLibraryFolders()).isEmpty();

        List<User> users = userService.getAll();
        assertThat(users).hasSize(1);
        assertThat(users).first().satisfies(user -> {
            assertThat(user.getName()).isEqualTo("Foo Bar");
            assertThat(user.getEmail()).isEqualTo("foo@bar.com");
            assertThat(user.getPassword()).isNotNull();
            assertThat(user.getRoles()).containsExactlyInAnyOrder(Role.USER, Role.ADMIN);
        });

        assertThat(installationService.getInstallation()).hasValueSatisfying(installation -> {
            assertThat(installation.getCreationDate()).isNotNull();
            assertThat(installation.getUpdateDate()).isNull();
            assertThat(installation.getVersion()).isNotNull();
        });

        ResponseEntity<InstallationStatusDto> statusResponse = restTemplate.getForEntity("/api/installation/status", InstallationStatusDto.class);

        assertThat(statusResponse.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(statusResponse.getBody()).satisfies(installationStatus ->
                assertThat(installationStatus.isInstalled()).isTrue());
    }

    @Test
    public void shouldValidateInstallationCommand() {

        InstallationCommandDto command = new InstallationCommandDto()
                .setInstallationSecret("invalidSecret")
                .setLibraryFolders(ImmutableList.of(LibraryFolderDto.of(new File("notExistingFile"))))
                .setAdminName(" ")
                .setAdminEmail("invalidEmail")
                .setAdminPassword(" ")
                .setRepeatAdminPassword("notMatchingPassword");

        ResponseEntity<ErrorDto> response = restTemplate.postForEntity("/api/installation", command, ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(Code.VALIDATION);
            assertThat(error.getFieldViolations().size()).isGreaterThanOrEqualTo(6);
            assertThat(error.getFieldViolations().stream()
                    .map(ErrorDto.FieldViolation::getField).distinct())
                    .containsExactlyInAnyOrder(
                            "installationSecret", 
                            "libraryFolders[0].path", 
                            "adminName", 
                            "adminEmail", 
                            "adminPassword",
                            "repeatAdminPassword"
                    );
        });
    }

    @Test
    public void shouldFailIfAlreadyInstalled() throws SecretNotFoundException, IOException {

        InstallationCommandDto command = new InstallationCommandDto()
                .setInstallationSecret(installationSecretService.fetchInstallationSecret())
                .setLibraryFolders(emptyList())
                .setAdminName("Foo Bar")
                .setAdminEmail("foo@bar.com")
                .setAdminPassword("somePassword")
                .setRepeatAdminPassword("somePassword");
        restTemplate.postForEntity("/api/installation", command, Void.class);

        ResponseEntity<ErrorDto> response = restTemplate.postForEntity("/api/installation", command, ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(errorDto -> 
                assertThat(errorDto.getCode()).isSameAs(Code.BAD_REQUEST));
    }
}
