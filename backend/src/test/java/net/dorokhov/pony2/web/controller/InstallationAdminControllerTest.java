package net.dorokhov.pony2.web.controller;

import net.dorokhov.pony2.ApiTemplate;
import net.dorokhov.pony2.InstallingIntegrationTest;
import net.dorokhov.pony2.api.installation.domain.Installation;
import net.dorokhov.pony2.core.installation.repository.InstallationRepository;
import net.dorokhov.pony2.web.dto.AuthenticationDto;
import net.dorokhov.pony2.web.dto.ErrorDto;
import net.dorokhov.pony2.web.dto.InstallationDto;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import static org.assertj.core.api.Assertions.assertThat;

public class InstallationAdminControllerTest extends InstallingIntegrationTest {
    
    @Autowired
    private ApiTemplate apiTemplate;
    
    @Autowired
    private InstallationRepository installationRepository;

    @Test
    public void shouldGetInstallation() {

        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<InstallationDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/installation", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), InstallationDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(installationDto -> {
            Installation installation = installationRepository.findAll().getFirst();
            assertThat(installationDto.getCreationDate()).isEqualTo(installation.getCreationDate());
            assertThat(installationDto.getUpdateDate()).isEqualTo(installation.getUpdateDate());
            assertThat(installationDto.getVersion()).isEqualTo(installation.getVersion());
        });
    }

    @Test
    public void shouldFailGettingInstallationIfNotInstalled() {

        installationRepository.deleteAll();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();

        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/installation", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getAccessToken()), ErrorDto.class);

        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments()).first().isEqualTo("Installation");
        });
    }
}
