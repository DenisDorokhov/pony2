package net.dorokhov.pony2.web.controller;

import com.google.common.collect.ImmutableList;
import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.ConfigDto;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.LibraryFolderDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

import static org.assertj.core.api.Assertions.assertThat;

public class ConfigAdminControllerTest extends InstallingIntegrationTest {

    @Autowired
    private ApiTemplate apiTemplate;

    @Test
    public void shouldGetConfig() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ConfigDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/config", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ConfigDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(config -> {
            assertThat(config.getLibraryFolders()).hasSize(1);
            assertThat(config.getLibraryFolders()).first().satisfies(libraryFolderDto -> 
                    assertThat(libraryFolderDto.getPath()).isEqualTo(libraryFolder.getAbsolutePath()));
        });
    }

    @Test
    public void shouldSaveConfig() throws IOException {

        File newLibraryFolder = Files.createDirectory(tempFolder.resolve("newLibraryFolder")).toFile();
        ConfigDto newConfig = new ConfigDto()
                .setLibraryFolders(ImmutableList.of(LibraryFolderDto.of(newLibraryFolder)));
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ConfigDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/config", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(newConfig, authentication.getAccessToken()), ConfigDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(config -> {
            assertThat(config.getLibraryFolders()).hasSize(1);
            assertThat(config.getLibraryFolders()).first().satisfies(libraryFolderDto ->
                    assertThat(libraryFolderDto.getPath()).isEqualTo(newLibraryFolder.getAbsolutePath()));
        });
    }

    @Test
    public void shouldValidateConfig() {

        ConfigDto config = new ConfigDto()
                .setLibraryFolders(ImmutableList.of(LibraryFolderDto.of(new File("notExistingFile"))));
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/config", HttpMethod.PUT,
                apiTemplate.createHeaderRequest(config, authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.VALIDATION);
            assertThat(error.getFieldViolations()).hasSize(1);
            assertThat(error.getFieldViolations()).first().satisfies(fieldViolation ->
                    assertThat(fieldViolation.getField()).isEqualTo("libraryFolders[0].path"));
        });
    }
}
