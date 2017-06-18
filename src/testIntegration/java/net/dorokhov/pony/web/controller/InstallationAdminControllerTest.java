package net.dorokhov.pony.web.controller;

import net.dorokhov.pony.ApiTemplate;
import net.dorokhov.pony.InstallingIntegrationTest;
import net.dorokhov.pony.installation.domain.Installation;
import net.dorokhov.pony.installation.repository.InstallationRepository;
import net.dorokhov.pony.web.domain.AuthenticationDto;
import net.dorokhov.pony.web.domain.ErrorDto;
import net.dorokhov.pony.web.domain.InstallationDto;
import org.junit.Test;
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
    public void shouldGetInstallation() throws Exception {
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<InstallationDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/installation", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), InstallationDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.OK);
        assertThat(response.getBody()).satisfies(installationDto -> {
            Installation installation = installationRepository.findAll().get(0);
            assertThat(installationDto.getCreationDate()).isEqualTo(installation.getCreationDate());
            assertThat(installationDto.getUpdateDate()).isEqualTo(installation.getUpdateDate());
            assertThat(installationDto.getVersion()).isEqualTo(installation.getVersion());
        });
    }

    @Test
    public void shouldFailGettingInstallationIfNotInstalled() throws Exception {
        installationRepository.deleteAll();
        AuthenticationDto authentication = apiTemplate.authenticateAdmin();
        ResponseEntity<ErrorDto> response = apiTemplate.getRestTemplate().exchange(
                "/api/admin/installation", HttpMethod.GET,
                apiTemplate.createHeaderRequest(authentication.getToken()), ErrorDto.class);
        assertThat(response.getStatusCode()).isSameAs(HttpStatus.NOT_FOUND);
        assertThat(response.getBody()).satisfies(error -> {
            assertThat(error.getCode()).isSameAs(ErrorDto.Code.NOT_FOUND);
            assertThat(error.getArguments()).first().isEqualTo("Installation");
        });
    }
}
